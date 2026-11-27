package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AndNode Unit Tests")
public class AndNodeTest {

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
		assertNull(new AndNode().getChild());
	}

	@Test
	@DisplayName("Default constructor leaves secondChild as null")
	void testDefaultSecondChildIsNull() {
		assertNull(new AndNode().getSecondChild());
	}

	@ParameterizedTest
	@CsvSource({
			"true, true, true",
			"true, false, false",
			"false, true, false",
			"false, false, false"
	})
	@DisplayName("matches implements logical AND truth table")
	void testMatchesTruthTable(boolean left, boolean right, boolean expected) {
		AndNode node = new AndNode();
		node.setChild(stubNode(left, "L"));
		node.setSecondChild(stubNode(right, "R"));
		assertEquals(expected, node.matches("target"));
	}

	@Test
	@DisplayName("matches short-circuits secondChild when first child returns false")
	void testMatchesShortCircuitsOnFalse() {
		AtomicBoolean secondCalled = new AtomicBoolean(false);
		AndNode node = new AndNode();
		node.setChild(stubNode(false, "L"));
		node.setSecondChild(trackingNode(true, secondCalled));
		node.matches("target");
		assertFalse(secondCalled.get());
	}

	@Test
	@DisplayName("matches evaluates secondChild when first child returns true")
	void testMatchesEvaluatesSecondChildOnTrue() {
		AtomicBoolean secondCalled = new AtomicBoolean(false);
		AndNode node = new AndNode();
		node.setChild(stubNode(true, "L"));
		node.setSecondChild(trackingNode(true, secondCalled));
		node.matches("target");
		assertTrue(secondCalled.get());
	}

	@Test
	@DisplayName("toString formats both children with AND")
	void testToStringFormat() {
		AndNode node = new AndNode();
		node.setChild(stubNode(true, "A"));
		node.setSecondChild(stubNode(false, "B"));
		assertEquals("(A AND B)", node.toString());
	}

	@Disabled("Bug conocido: AndNode.matches lanza NullPointerException si child o secondChild son null")
	@Test
	@DisplayName("matches returns false when children are null")
	void testMatchesNullChildrenReturnsFalse() {
		AndNode node = new AndNode();
		assertFalse(node.matches("target"));
	}

	@Disabled("Bug conocido: AndNode.toString lanza NullPointerException si child o secondChild son null")
	@Test
	@DisplayName("toString handles null children without throwing NullPointerException")
	void testToStringNullChildren() {
		AndNode node = new AndNode();
		assertNotNull(node.toString());
	}
}
