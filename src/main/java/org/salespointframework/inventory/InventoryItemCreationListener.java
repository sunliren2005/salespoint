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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.PrePersist;

import org.salespointframework.catalog.Product;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor
class InventoryItemCreationListener {

	private static final String UNIQUE_ITEM_ALREADY_EXISTS = "Trying to persist unique inventory item for %s. The following item(s) already exist: %s.";

	private final @NonNull Inventory<?> inventory;

	/**
	 * Verifies that there's no other {@link UniqueInventoryItem} present for the product that the to be persisted
	 * {@link AbstractInventoryItem} belongs to or there's no {@link AbstractInventoryItem} for the referred to
	 * {@link Product} in general if the item to be persisted is a {@link UniqueInventoryItem}.
	 * 
	 * @param item must not be {@literal null}.
	 */
	@PrePersist
	public void prePersist(AbstractInventoryItem<?> item) {

		Assert.notNull(item, "Inventory item must not be null!");

		InventoryItems<?> items = inventory.findByProduct(item.getProduct());

		items.resolveForUnique(Function.identity()) //
				.orMultiple(existing -> assertNonUniqueItem(item, existing)) //
				.filter(existing -> existing.isDifferentItemForSameProduct(item)) //
				.ifPresent(existing -> {
					throw new IllegalStateException(String.format(UNIQUE_ITEM_ALREADY_EXISTS, item, existing));
				});
	}

	private static Optional<UniqueInventoryItem> assertNonUniqueItem(AbstractInventoryItem<?> item,
			Streamable<?> existing) {

		Assert.state(!UniqueInventoryItem.class.isInstance(item), //
				() -> String.format(UNIQUE_ITEM_ALREADY_EXISTS, item.getProduct(),
						existing.stream().collect(Collectors.toList())));

		return Optional.empty();
	}
}
