/*
 * Copyright 2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import javax.persistence.PrePersist;

import org.salespointframework.catalog.Product;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor
class InventoryItemCreationListener {

	private static final String UNIQUE_ITEM_ALREADY_EXISTS = "Trying to persist unique inventory item for %s. The following item(s) already exist: %s.";

	private final @NonNull Inventory<?> inventory;
	private final @NonNull FooUniqueInventory<UniqueInventoryItem> fooUniqueInventory;
	private final @NonNull FooInventory<InventoryItem> fooInventory;

	/**
	 * Verifies that there's no other {@link UniqueInventoryItem} present for the product that the to be persisted
	 * {@link AbstractInventoryItem} belongs to or there's no {@link AbstractInventoryItem} for the referred to
	 * {@link Product} in general if the item to be persisted is a {@link UniqueInventoryItem}.
	 * 
	 * @param item must not be {@literal null}.
	 */
	@PrePersist
	public void verify(AbstractInventoryItem<?> item) {

		assertNonUniqueItem(item);

		if (UniqueInventoryItem.class.isInstance(item)) {

			InventoryItems<InventoryItem> existing = fooInventory.findByProductIdentifier(item.getProduct().getId());

			if (existing.isEmpty()) {
				return;
			}

			throw new IllegalStateException(String.format(UNIQUE_ITEM_ALREADY_EXISTS, item, existing.stream()//
					.map(Object::toString) //
					.collect(Collectors.joining(", "))));
		}
	}

	private void assertNonUniqueItem(AbstractInventoryItem<?> item) {

		fooUniqueInventory.findByProduct(item.getProduct()) //
				.filter(it -> it.isDifferentItemForSameProduct(item)) //
				.ifPresent(existing -> {
					throw new IllegalStateException(String.format(UNIQUE_ITEM_ALREADY_EXISTS, item, existing));
				});
	}
}
