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

import java.util.Optional;

import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Oliver Gierke
 */
public interface FooUniqueInventory<T extends UniqueInventoryItem>
		extends FooAbstractInventory<T>, CrudRepository<T, InventoryItemIdentifier> {

	@Query("select i from #{#entityName} i where i.product.id = ?1")
	Optional<T> findByProductIdentifier(ProductIdentifier productIdentifier);

	default Optional<T> findByProduct(Product product) {
		return findByProductIdentifier(product.getId());
	}
}
