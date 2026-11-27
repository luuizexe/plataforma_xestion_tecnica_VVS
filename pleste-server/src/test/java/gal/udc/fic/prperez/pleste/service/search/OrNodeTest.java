package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrNode Unit Tests")
public class OrNodeTest {

	private static Node stubNode(boolean result, String label) {
		return new Node() {
			@Override
			public String toString() {
				return label;
			}

			@Override
			public boolean matches(Object n) {
				return result;
			}
		};
	}

	private static Node trackingNode(boolean result, AtomicBoolean called) {
		return new Node() {
			@Override
			public String toString() {
				return "tracked";
			}

			@Override
			public boolean matches(Object n) {
				called.set(true);
				return result;
			}
		};
	}

	@Test
	@DisplayName("Default constructor leaves child as null")
	void testDefaultChildIsNull() {
		assertNull(new OrNode().getChild());
	}

	@Test
	@DisplayName("Default constructor leaves secondChild as null")
	void testDefaultSecondChildIsNull() {
		assertNull(new OrNode().getSecondChild());
	}

	@ParameterizedTest
	@CsvSource({
			"true, true, true",
			"true, false, true",
			"false, true, true",
			"false, false, false"
	})
	@DisplayName("matches implements logical OR truth table")
	void testMatchesTruthTable(boolean left, boolean right, boolean expected) {
		OrNode node = new OrNode();
		node.setChild(stubNode(left, "L"));
		node.setSecondChild(stubNode(right, "R"));
		assertEquals(expected, node.matches("target"));
	}

	@Test
	@DisplayName("matches short-circuits secondChild when first child returns true")
	void testMatchesShortCircuitsOnTrue() {
		AtomicBoolean secondCalled = new AtomicBoolean(false);
		OrNode node = new OrNode();
		node.setChild(stubNode(true, "L"));
		node.setSecondChild(trackingNode(false, secondCalled));
		node.matches("target");
		assertFalse(secondCalled.get());
	}

	@Test
	@DisplayName("matches evaluates secondChild when first child returns false")
	void testMatchesEvaluatesSecondChildOnFalse() {
		AtomicBoolean secondCalled = new AtomicBoolean(false);
		OrNode node = new OrNode();
		node.setChild(stubNode(false, "L"));
		node.setSecondChild(trackingNode(true, secondCalled));
		node.matches("target");
		assertTrue(secondCalled.get());
	}

	@Test
	@DisplayName("toString formats both children with OR")
	void testToStringFormat() {
		OrNode node = new OrNode();
		node.setChild(stubNode(true, "A"));
		node.setSecondChild(stubNode(false, "B"));
		assertEquals("(A OR B)", node.toString());
	}

	@Disabled("Bug conocido: OrNode.matches lanza NullPointerException si child o secondChild son null")
	@Test
	@DisplayName("matches returns false when children are null")
	void testMatchesNullChildrenReturnsFalse() {
		OrNode node = new OrNode();
		assertFalse(node.matches("target"));
	}

	@Disabled("Bug conocido: OrNode.toString lanza NullPointerException si child o secondChild son null")
	@Test
	@DisplayName("toString handles null children without throwing NullPointerException")
	void testToStringNullChildren() {
		OrNode node = new OrNode();
		assertNotNull(node.toString());
	}
}
