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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;

import org.salespointframework.catalog.Product;
import org.salespointframework.core.AbstractEntity;
import org.salespointframework.quantity.Quantity;
import org.springframework.util.Assert;

/**
 * @author Paul Henke
 * @author Oliver Gierke
 */
@Entity
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@EntityListeners(InventoryItemCreationListener.class)
public abstract class AbstractInventoryItem<T extends AbstractInventoryItem<T>>
		extends AbstractEntity<InventoryItemIdentifier> {

	@EmbeddedId //
	@AttributeOverride(name = "id", column = @Column(name = "ITEM_ID")) //
	private final InventoryItemIdentifier inventoryItemIdentifier = new InventoryItemIdentifier();

	@Getter //
	private Quantity quantity;

	/**
	 * Creates a new {@link UniqueInventoryItem} for the given {@link Product} and {@link Quantity}.
	 * 
	 * @param quantity the initial {@link Quantity} for this {@link UniqueInventoryItem}, must not be {@literal null}.
	 */
	public AbstractInventoryItem(Product product, Quantity quantity) {

		Assert.notNull(product, "Product must be not null!");
		Assert.notNull(quantity, "Quantity must be not null!");

		product.verify(quantity);

		this.quantity = quantity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#getId()
	 */
	public final InventoryItemIdentifier getId() {
		return inventoryItemIdentifier;
	}

	/**
	 * Returns whether the {@link UniqueInventoryItem} is available in exactly or more of the given quantity.
	 * 
	 * @param quantity must not be {@literal null}.
	 * @return
	 */
	public boolean hasSufficientQuantity(Quantity quantity) {
		return !this.quantity.subtract(quantity).isNegative();
	}

	/**
	 * Decreases the quantity of the current {@link UniqueInventoryItem} by the given {@link Quantity}.
	 * 
	 * @param quantity must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public T decreaseQuantity(Quantity quantity) {

		Assert.notNull(quantity, "Quantity must not be null!");
		Assert.isTrue(this.quantity.isGreaterThanOrEqualTo(quantity),
				String.format("Insufficient quantity! Have %s but was requested to reduce by %s.", this.quantity, quantity));

		getProduct().verify(quantity);

		this.quantity = this.quantity.subtract(quantity);

		return (T) this;
	}

	/**
	 * Increases the quantity of the current {@link UniqueInventoryItem} by the given {@link Quantity}.
	 * 
	 * @param quantity must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public T increaseQuantity(Quantity quantity) {

		Assert.notNull(quantity, "Quantity must not be null!");
		getProduct().verify(quantity);

		this.quantity = this.quantity.add(quantity);

		return (T) this;
	}

	public boolean belongsTo(Product product) {
		return this.getProduct().equals(product);
	}

	public boolean isDifferentItemForSameProduct(AbstractInventoryItem<?> other) {
		return !this.equals(other) && this.belongsTo(other.getProduct());
	}

	protected abstract Product getProduct();

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		Product product = getProduct();

		return String.format("%s(%s) for Product(%s, \"%s\") with quantity %s", //
				getClass().getSimpleName(), getId(), product.getId(), product.getName(), getQuantity());
	}
}
