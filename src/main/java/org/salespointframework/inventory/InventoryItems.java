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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.salespointframework.catalog.Product;
import org.salespointframework.quantity.Quantity;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * API to handle both {@link AbstractInventoryItem}s and {@link UniqueInventoryItem}s. Offers convenience methods to
 * handle cases in which a lookup of {@link AbstractInventoryItem}s for a {@link Product} return multiple items or a
 * {@link UniqueInventoryItem}.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryItems<T extends AbstractInventoryItem<T>> implements Streamable<T> {

	private final @NonNull Streamable<T> items;
	private final @Nullable Optional<T> item;

	/**
	 * Creates a new {@link InventoryItems} for the given {@link Streamable}.
	 * 
	 * @param source
	 * @return
	 */
	public static <S extends AbstractInventoryItem<S>> InventoryItems<S> of(Streamable<S> source) {

		List<S> list = source.stream().collect(Collectors.toList());

		if (list.size() == 0) {
			return new InventoryItems<S>(source, Optional.empty());
		}

		S item = list.get(0);

		if (list.size() > 1 || !UniqueInventoryItem.class.isInstance(item)) {
			return new InventoryItems<S>(source, null);
		}

		return new InventoryItems<S>(source, Optional.of(list.get(0)));
	}

	/**
	 * Returns whether the result is unique, i.e. there's either no or exactly one {@link AbstractInventoryItem} in the
	 * result.
	 * 
	 * @return
	 */
	public boolean isUnique() {
		return item != null;
	}

	/**
	 * Executes the given consumer if there's a unique result and it's actually present.
	 * 
	 * @param consumer must not be {@literal null}.
	 */
	void ifUniquePresent(Consumer<UniqueInventoryItem> consumer) {

		Assert.notNull(consumer, "Consumer must not be null!");

		item.map(UniqueInventoryItem.class::cast).ifPresent(consumer);
	}

	public <S> Optional<S> mapUniqueIfPresent(Function<UniqueInventoryItem, S> mapper) {
		return item != null ? item.map(UniqueInventoryItem.class::cast).map(mapper) : Optional.empty();
	}

	/**
	 * Creates a new {@link Resolver} carrying the given function for the case that a unique {@link AbstractInventoryItem}
	 * is contained. Complete the resolution via {@link Resolver#orMultiple(Function)}.
	 * 
	 * @param onUnique
	 * @return
	 */
	public <S> Resolver<S> resolveForUnique(Function<Optional<UniqueInventoryItem>, S> onUnique) {
		return new Resolver<>(onUnique);
	}

	/**
	 * Filters the current {@link InventoryItems} to only those matching the given {@link Predicate}.
	 * 
	 * @return will never be {@literal null}.
	 */
	public InventoryItems<T> filter(Predicate<? super T> filter) {
		return InventoryItems.of(items.filter(filter));
	}

	/**
	 * Returns the total quantity of all the {@link AbstractInventoryItem}s contained.
	 * 
	 * @return will never be {@literal null}.
	 */
	public Quantity getTotalQuantity() {

		return stream() //
				.map(AbstractInventoryItem::getQuantity) //
				.reduce(Quantity::add) //
				.orElse(Quantity.NONE);
	}

	/**
	 * Returns the {@link UniqueInventoryItem} contained in this
	 * 
	 * @return
	 */
	public Optional<T> toUnique() {

		Assert.state(isUnique(), "Expected unique inventory item but got multiple ones!");

		return item;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return items.iterator();
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public class Resolver<S> {

		private final Function<Optional<UniqueInventoryItem>, S> uniqueMapper;

		/**
		 * Concludes a resolution with the given {@link Function} and applies either the unique mapper registered in case
		 * there's a unique result present or the given mapper in case there are multiple ones present.
		 * 
		 * @param mapper
		 * @return
		 */
		public S orMultiple(Function<Streamable<T>, S> mapper) {

			return isUnique() //
					? uniqueMapper.apply(item.map(UniqueInventoryItem.class::cast)) //
					: mapper.apply(items);
		}

		/**
		 * Concludes the resolution by registering the given {@link Supplier} to produce an exception in case there's no
		 * unique result present.
		 * 
		 * @param supplier
		 * @return
		 */
		public S onMultipleThrow(Supplier<? extends RuntimeException> supplier) {

			if (!isUnique()) {
				throw supplier.get();
			}

			return uniqueMapper.apply(item.map(UniqueInventoryItem.class::cast));
		}
	}
}
