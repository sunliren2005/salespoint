/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.salespointframework.inventory;

import static org.salespointframework.order.OrderCompletionReport.OrderLineCompletion.*;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.order.Order.OrderCancelled;
import org.salespointframework.order.Order.OrderCompleted;
import org.salespointframework.order.OrderCompletionFailure;
import org.salespointframework.order.OrderCompletionReport;
import org.salespointframework.order.OrderCompletionReport.OrderLineCompletion;
import org.salespointframework.order.OrderLine;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * {@link ApplicationListener} for {@link OrderCompleted} events to verify that sufficient amounts of the
 * {@link Product} the {@link OrderLine}s contained in the {@link Order} point to are available in the
 * {@link Inventory}.
 * 
 * @author Oliver Gierke
 * @since 6.3
 */
@Component
@RequiredArgsConstructor
public class InventoryOrderEventListener {

	private static final String NOT_ENOUGH_STOCK = "Number of items requested by the OrderLine is greater than the number available in the Inventory. Please re-stock.";
	private static final String NO_INVENTORY_ITEM = "No inventory item with given product indentifier found in inventory. Have you initialized your inventory? Do you need to re-stock it?";

	// private final @NonNull Inventory<UniqueInventoryItem> inventory;
	private final @NonNull List<LineItemFilter> filters;

	private final @NonNull FooInventory<InventoryItem> fooInventory;
	private final @NonNull FooUniqueInventory<UniqueInventoryItem> fooUniqueInventory;

	/**
	 * Invokes {@link Inventory} checks for all {@link OrderLine} of the {@link Order} in the given {@link OrderCompleted}
	 * event.
	 * 
	 * @param event must not be {@literal null}.
	 * @throws OrderCompletionFailure in case any of the {@link OrderLine} items contained in the order and supported by
	 *           the configured {@link LineItemFilter} is not available in sufficient quantity.
	 */
	@EventListener
	public void on(OrderCompleted event) throws OrderCompletionFailure {

		Assert.notNull(event, "OrderCompletedEvent must not be null!");

		Order order = event.getOrder();

		List<OrderLineCompletion> collect = order.getOrderLines() //
				.map(this::verify)//
				.toList();

		OrderCompletionReport.forCompletions(order, collect) //
				.onError(OrderCompletionFailure::new);
	}

	/**
	 * Rolls back the stock decreases handled for {@link OrderCompleted} events.
	 * 
	 * @param event must not be {@literal null}.
	 */
	@EventListener
	public void on(OrderCancelled event) {

		Order order = event.getOrder();

		if (!order.isCompleted()) {
			return;
		}

		order.getOrderLines() //
				.flatMap(this::updateStockFor) //
				.forEach(fooUniqueInventory::save);
	}

	/**
	 * Verifies the given {@link OrderLine} for sufficient stock in the {@link Inventory}.
	 * 
	 * @param orderLine must not be {@literal null}.
	 * @return
	 */
	private OrderLineCompletion verify(OrderLine orderLine) {

		Assert.notNull(orderLine, "OrderLine must not be null!");

		if (!LineItemFilter.shouldBeHandled(orderLine, filters)) {
			return OrderLineCompletion.success(orderLine);
		}

		ProductIdentifier identifier = orderLine.getProductIdentifier();

		Optional<UniqueInventoryItem> optional = fooUniqueInventory.findByProductIdentifier(identifier);

		return optional.map(it -> verifyUnique(it, orderLine)) //
				.orElseGet(() -> {

					Streamable<InventoryItem> items = fooInventory.findByProductIdentifier(identifier);
					return items.isEmpty() ? error(orderLine, NO_INVENTORY_ITEM) : skipped(orderLine);
				});

		// fooInventory.findByProductIdentifier(identifier);

		// InventoryItems<? extends AbstractInventoryItem<?>> inventoryItem = inventory.findByProductIdentifier(identifier);
		//
		// return inventoryItem //
		// .resolveForUnique(it -> verifyUnique(it, orderLine)) //
		// .orMultiple(__ -> skipped(orderLine));
	}

	private OrderLineCompletion verifyUnique(UniqueInventoryItem item, OrderLine orderLine) {
		return hasSufficientQuantity(item, orderLine)

				// return item.map(it -> hasSufficientQuantity(it, orderLine)) //
				// .orElseGet(() -> error(orderLine, NO_INVENTORY_ITEM)) //
				.onSuccess(it -> fooUniqueInventory.save(item.decreaseQuantity(it.getQuantity())));
	}

	private Stream<UniqueInventoryItem> updateStockFor(OrderLine orderLine) {

		ProductIdentifier productIdentifier = orderLine.getProductIdentifier();

		Optional<UniqueInventoryItem> item = fooUniqueInventory.findByProductIdentifier(productIdentifier);
		item.map(it -> it.increaseQuantity(orderLine.getQuantity()));

		if (!item.isPresent() && fooInventory.findByProductIdentifier(productIdentifier).isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Couldn't find InventoryItem for product %s!", productIdentifier));
		}

		return Optionals.toStream(item);
		//
		// return inventory.findByProductIdentifier(productIdentifier) //
		// .mapUniqueIfPresent(it -> it.increaseQuantity(orderLine.getQuantity())) //
		// .orElseThrow(() -> new IllegalArgumentException(
		// String.format("Couldn't find InventoryItem for product %s!", productIdentifier)));
	}

	private static OrderLineCompletion hasSufficientQuantity(UniqueInventoryItem item, OrderLine orderLine) {

		return item.hasSufficientQuantity(orderLine.getQuantity()) //
				? success(orderLine) //
				: error(orderLine, NOT_ENOUGH_STOCK);
	}
}
