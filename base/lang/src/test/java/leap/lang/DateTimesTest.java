/*
 * Copyright 2014 the original author or authors.
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
package leap.lang;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;

import leap.junit.TestBase;

public class DateTimesTest extends TestBase {
	
	@Test
	public void testParseLocalDateWithDefaultFormat() {
		LocalDate d = LocalDate.of(2014, 11, 01);
		
		assertEquals(d,DateTimes.tryParseLocalDate("2014-11-01"));
		assertEquals(d,DateTimes.tryParseLocalDate("2014-11-01T10:01:01"));
		assertEquals(d,DateTimes.tryParseLocalDate("2014-11-01 10:01:01"));
	}
	
	@Test
	public void testParseLocalTimeWithDefaultFormat() {
		LocalTime t = LocalTime.of(10, 01, 01);
		
		assertEquals(t,DateTimes.tryParseLocalTime("10:01:01"));
		assertEquals(LocalTime.of(10, 01, 00),DateTimes.tryParseLocalTime("10:01"));
		assertEquals(t,DateTimes.tryParseLocalTime("2014-11-01T10:01:01"));
		assertEquals(t,DateTimes.tryParseLocalTime("2014-11-01 10:01:01"));
	}
	
	@Test
	public void testParseLocalDateTimeWithDefaultFormat() {
		LocalDateTime dt = LocalDateTime.of(2014, 11, 1, 10, 01, 01);
		
		assertEquals(dt,DateTimes.tryParseLocalDateTime("2014-11-01T10:01:01"));
		assertEquals(dt,DateTimes.tryParseLocalDateTime("2014-11-01 10:01:01"));
		assertEquals(LocalDateTime.of(2014, 11, 1, 00, 00, 00),DateTimes.tryParseLocalDateTime("2014-11-01"));
	}

}
