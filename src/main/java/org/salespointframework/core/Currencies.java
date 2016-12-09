package org.salespointframework.core;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.javamoney.moneta.Money;

/**
 * Interface to contain {@link CurrencyUnit} constants.
 * 
 * @author Oliver Gierke
 */
public interface Currencies {

	public static final CurrencyUnit EURO = Monetary.getCurrency("EUR");

	public static final MonetaryAmount ZERO_EURO = Money.of(0, EURO);
}