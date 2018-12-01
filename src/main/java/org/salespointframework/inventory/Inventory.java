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

import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.quantity.Quantity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Repository interface for {@link UniqueInventoryItem}s.
 * 
 * @author Oliver Gierke
 */
// tag::inventory[]
public interface Inventory<T extends AbstractInventoryItem<T>> extends CrudRepository<T, InventoryItemIdentifier> {

	/**
	 * Returns all {@link UniqueInventoryItem}s that are out of stock (i.e. the {@link Quantity}'s amount is equal or less
	 * than zero).
	 * 
	 * @return will never be {@literal null}.
	 */
	@Query("select i from #{#entityName} i where i.quantity.amount <= 0")
	Streamable<T> findItemsOutOfStock();

	/**
	 * Returns the {@link AbstractInventoryItem}s for the {@link Product} with the given identifier.
	 * 
	 * @param productIdentifier must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	@Query("select i from #{#entityName} i where i.product.id = ?1")
	Streamable<T> findAllByProductIdentifier(ProductIdentifier productIdentifier);

	/**
	 * Returns all {@link InventoryItems} for the {@link Product} with the given identifier. If you expect multiple
	 * {@link AbstractInventoryItem} instances to be contained in the result set anyway, just {@link InventoryItems#stream()} over
	 * them. In case you're expecting a {@link UniqueInventoryItem}, use the dedicated methods on this type, to define
	 * resolutions, or {@link InventoryItems#mapUniqueIfPresent(java.util.function.Function)} etc.
	 * 
	 * @param productIdentifier must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default InventoryItems<T> findByProductIdentifier(ProductIdentifier productIdentifier) {

		Assert.notNull(productIdentifier, "ProductIdentifier must not be null!");

		return InventoryItems.of(findAllByProductIdentifier(productIdentifier));
	}

	/**
	 * Returns the {@link InventoryItems} for the given {@link Product}.
	 * 
	 * @param product must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see #findByProductIdentifier(ProductIdentifier) for details on how to work with {@link InventoryItems}.
	 */
	default InventoryItems<T> findByProduct(Product product) {

		Assert.notNull(product, "Product must not be null!");

		return findByProductIdentifier(product.getId());
	}
}
// end::inventory[]
