/*
 * Copyright 2017-2019 the original author or authors.
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
package org.salespointframework.support;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.junit.MatcherAssert.*;

import java.math.BigDecimal;
import java.util.Locale;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MonetaryAmountAttributeConverter}
 *
 * @author Oliver Gierke
 */
class MonetaryAmountAttributeConverterTest {

	MonetaryAmountAttributeConverter converter = new MonetaryAmountAttributeConverter();

	@Test // #156
	void handlesNullValues() {

		assertThat(converter.convertToDatabaseColumn(null), is(nullValue()));
		assertThat(converter.convertToEntityAttribute(null), is(nullValue()));
	}

	@Test // #156
	void handlesSimpleValue() {

		assertThat(converter.convertToDatabaseColumn(Money.of(1.23, "EUR")), is("EUR 1.23"));
		assertThat(converter.convertToEntityAttribute("EUR 1.23"), is(Money.of(1.23, "EUR")));
	}

	@Test // #156
	void handlesNegativeValues() {

		assertThat(converter.convertToDatabaseColumn(Money.of(-1.20, "USD")), is("USD -1.2"));
		assertThat(converter.convertToEntityAttribute("USD -1.2"), is(Money.of(-1.20, "USD")));
	}

	@Test // #156
	void doesNotRoundValues() {
		assertThat(converter.convertToDatabaseColumn(Money.of(1.23456, "EUR")), is("EUR 1.23456"));
	}

	@Test // #156
	void doesNotFormatLargeValues() {
		assertThat(converter.convertToDatabaseColumn(Money.of(123456, "EUR")), is("EUR 123456"));
	}

	@Test // #156
	void deserializesFormattedValues() {

		MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(Locale.ROOT);

		MonetaryAmount source = Money.of(123456.78, "EUR");
		String formatted = format.format(source);

		// TODO - Remove before merge

		System.out.println(formatted); // "EUR 123,456.78", space is a 32 on JDK 8, a 160 on JDK 9 and 10
		System.out.println((int) formatted.toCharArray()[3]); // JDK 8: 32, JDK 9: JDK 10:

		MonetaryAmount foo = format.parse(formatted);
		MonetaryAmount result = format.parse("EUR 123,456.78"); // Space is char of 32 (standard space)

		assertThat(converter.convertToEntityAttribute(formatted), is(Money.of(123456.78, "EUR")));
	}

	@Test // #156
	void convertsValuesWithTrailingZerosCorrectly() {

		Money reference = Money.of(100.0, "EUR");

		assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(reference)), is(reference));
	}

	@Test // #156
	void convertsValuesWithTrailingZerosCorrectly2() {

		Money reference = Money.of(new BigDecimal("100.0"), "EUR");

		assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(reference)), is(reference));
	}
}
