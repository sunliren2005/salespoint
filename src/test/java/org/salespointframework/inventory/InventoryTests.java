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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import de.olivergierke.moduliths.test.ModuleTest;

import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.Catalog;
import org.salespointframework.catalog.Cookie;
import org.salespointframework.catalog.Product;
import org.salespointframework.core.Currencies;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link Inventory}.
 *
 * @author Oliver Gierke
 */
@Transactional
@ModuleTest(extraIncludes = "org.salespointframework.catalog")
class InventoryTests {

	@Autowired Inventory<UniqueInventoryItem> inventory;
	@Autowired Inventory<InventoryItem> items;
	@Autowired Catalog<Product> catalog;
	@Autowired EntityManager em;

	Cookie cookie;
	UniqueInventoryItem item;

	@BeforeEach
	void before() {

		cookie = catalog.save(new Cookie("Add Superkeks", Currencies.ZERO_EURO));
		item = inventory.save(new UniqueInventoryItem(cookie, Quantity.of(10)));

	}

	@Test
	void savesItemsCorrectly() {
		assertThat(inventory.save(item).getId(), is(notNullValue()));
	}

	@Test // #34
	void deletesItemsCorrectly() {

		inventory.deleteById(item.getId());

		assertThat(inventory.existsById(item.getId()), is(false));
	}

	@Test // #34
	void testExists() {
		assertThat(inventory.existsById(item.getId()), is(true));
	}

	@Test // #34
	void testGet() {

		Optional<UniqueInventoryItem> result = inventory.findById(item.getId());

		assertThat(result.isPresent(), is(true));
		assertThat(result.get(), is(item));
	}

	@Test // #34
	void testFindItemsByProduct() {

		InventoryItems<UniqueInventoryItem> result = inventory.findByProduct(cookie);

		assertThat(result.isUnique()).isTrue();
		result.ifUniquePresent(it -> assertThat(it).isEqualTo(item));
	}

	@Test // #34
	void testFindItemsByProductId() {

		InventoryItems<UniqueInventoryItem> result = inventory.findByProductIdentifier(cookie.getId());

		assertThat(result.isUnique()).isTrue();
		result.ifUniquePresent(it -> assertThat(it).isEqualTo(item));
	}

	@Test // #34
	void decreasesItemAndPersistsIt() {

		InventoryItems<UniqueInventoryItem> item = inventory.findByProduct(cookie);

		item.ifUniquePresent(it -> it.decreaseQuantity(Quantity.of(1)));

		// Trigger another finder to flush
		InventoryItems<?> result = inventory.findByProductIdentifier(cookie.getId());

		assertThat(result.isUnique()).isTrue();
		result.ifUniquePresent(it -> assertThat(it.getQuantity()).isEqualTo(Quantity.of(9)));
	}

	/**
	 * @see #68
	 */
	void rejectsNewInventoryItemForExistingProducts() {

		inventory.save(new UniqueInventoryItem(cookie, Quantity.of(10)));

		assertThatExceptionOfType(PersistenceException.class) //
				.isThrownBy(() -> em.flush()) //
				.withCauseExactlyInstanceOf(ConstraintViolationException.class);
	}

	@Test // #142
	void findsInventoryItemsOutOfStock() {

		assertThat(inventory.findItemsOutOfStock()).isEmpty();

		InventoryItems<UniqueInventoryItem> result = inventory.findByProduct(cookie);

		assertThat(result.isUnique()).isTrue();

		result.ifUniquePresent(item -> {

			item.decreaseQuantity(Quantity.of(10));
			assertThat(inventory.findItemsOutOfStock(), is(iterableWithSize(1)));
		});
	}

	@Test
	void looksUpMultipleInventoryItemsPerProduct() {

		Cookie otherCookie = catalog.save(new Cookie("Other cookie", Money.of(3, Currencies.EURO)));

		InventoryItem first = items.save(new InventoryItem(otherCookie, Quantity.of(5)));
		InventoryItem second = items.save(new InventoryItem(otherCookie, Quantity.of(3)));

		InventoryItems<InventoryItem> items = this.items.findByProduct(otherCookie);

		assertThat(items.isUnique()).isFalse();
		assertThat(items.isEmpty()).isFalse();
		assertThat(items).containsExactlyInAnyOrder(first, second);
		assertThat(items.getTotalQuantity()).isEqualTo(Quantity.of(8));
	}

	@Test
	void rejectsNewUniqueInventoryItemForAlreadyExistingUniqueInventoryItem() {

		Cookie otherCookie = catalog.save(new Cookie("Other cookie", Money.of(3, Currencies.EURO)));

		UniqueInventoryItem first = new UniqueInventoryItem(otherCookie, Quantity.of(5));
		UniqueInventoryItem second = new UniqueInventoryItem(otherCookie, Quantity.of(0));

		assertRejectsSecond(first, it -> inventory.save(it), second, it -> inventory.save(it));
	}

	@Test
	void rejectsNewUniqueInventoryItemForAlreadyExistingInventoryItem() {

		Cookie otherCookie = catalog.save(new Cookie("Other cookie", Money.of(3, Currencies.EURO)));

		InventoryItem first = new InventoryItem(otherCookie, Quantity.of(5));
		UniqueInventoryItem second = new UniqueInventoryItem(otherCookie, Quantity.of(0));

		assertRejectsSecond(first, it -> items.save(it), second, it -> inventory.save(it));
	}

	@Test
	void rejectsNewInventoryItemForAlreadyExistingUniqueInventoryItem() {

		Cookie otherCookie = catalog.save(new Cookie("Other cookie", Money.of(3, Currencies.EURO)));

		UniqueInventoryItem first = new UniqueInventoryItem(otherCookie, Quantity.of(0));
		InventoryItem second = new InventoryItem(otherCookie, Quantity.of(5));

		assertRejectsSecond(first, it -> inventory.save(it), second, it -> items.save(it));
	}

	@Test
	void singleInventoryItemIsNotConsideredUnique() throws Exception {

		Cookie otherCookie = catalog.save(new Cookie("Other cookie", Money.of(3, Currencies.EURO)));

		items.save(new InventoryItem(otherCookie, Quantity.of(2)));

		InventoryItems<InventoryItem> result = items.findByProduct(otherCookie);

		assertThat(result.isUnique()).isFalse();
		assertThat(result.isEmpty()).isFalse();
		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> result.toUnique());
	}

	private <S extends AbstractInventoryItem<S>, T extends AbstractInventoryItem<T>> void assertRejectsSecond(S first,
			Function<S, S> firstSaver, T second, Function<T, T> secondSaver) {

		S saved = firstSaver.apply(first);

		Throwable exception = catchThrowable(() -> secondSaver.apply(second));

		assertThat(exception).isInstanceOfSatisfying(InvalidDataAccessApiUsageException.class, outer -> {
			assertThat(outer.getCause()).isInstanceOfSatisfying(IllegalStateException.class, inner -> {
				assertThat(inner) //
						.hasMessageContaining(saved.getId().toString()) //
						.hasMessageContaining(second.getProduct().getId().toString());
			});
		});
	}
}
