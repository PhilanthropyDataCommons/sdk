package com.pdc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SemVerUtilsTest {
	@Test
	public void addsEachComponentWhenBothVersionsAreStable() {
		assertEquals("3.5.7", SemVerUtils.combineSemVer("1.2.3", "2.3.4"));
	}

	@Test
	public void foldsTheMajorIntoTheMinorWhenTheFirstVersionIsUnstable() {
		assertEquals("0.7.7", SemVerUtils.combineSemVer("0.2.3", "2.3.4"));
	}

	@Test
	public void foldsTheMajorIntoTheMinorWhenTheSecondVersionIsUnstable() {
		assertEquals("0.6.7", SemVerUtils.combineSemVer("1.2.3", "0.3.4"));
	}

	@Test
	public void combinesTheTemplateAndSpecificationVersions() {
		assertEquals("0.3.1", SemVerUtils.combineSemVer("0.0.1", "0.3.0"));
	}

	@Test
	public void ignoresComponentsBeyondThePatch() {
		assertEquals("2.2.3", SemVerUtils.combineSemVer("1.0.0", "1.2.3.4"));
	}

	@Test
	public void rejectsVersionsWithTooFewComponents() {
		assertThrows(
			IllegalArgumentException.class,
			() -> SemVerUtils.combineSemVer("1.2", "1.2.3")
		);
	}

	@Test
	public void rejectsVersionsWithNonNumericComponents() {
		assertThrows(
			NumberFormatException.class,
			() -> SemVerUtils.combineSemVer("1.2.x", "1.2.3")
		);
	}
}
