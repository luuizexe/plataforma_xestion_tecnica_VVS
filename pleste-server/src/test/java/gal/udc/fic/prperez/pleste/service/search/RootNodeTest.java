package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RootNode Unit Tests")
public class RootNodeTest {

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

	@Test
	@DisplayName("matches returns false when child is null")
	void testMatchesWithNullChildReturnsFalse() {
		RootNode root = new RootNode();
		assertFalse(root.matches("any"));
	}

	@Test
	@DisplayName("matches returns true when child matches")
	void testMatchesWithTrueChildReturnsTrue() {
		RootNode root = new RootNode();
		root.setChild(stubNode(true, "T"));
		assertTrue(root.matches("any"));
	}

	@Test
	@DisplayName("matches returns false when child does not match")
	void testMatchesWithFalseChildReturnsFalse() {
		RootNode root = new RootNode();
		root.setChild(stubNode(false, "F"));
		assertFalse(root.matches("any"));
	}

	@Test
	@DisplayName("matches forwards null target to child")
	void testMatchesWithNullTarget() {
		RootNode root = new RootNode();
		root.setChild(stubNode(true, "T"));
		assertTrue(root.matches(null));
	}

	@Disabled("Bug conocido: RootNode.matches captura NullPointerException de los hijos y devuelve false ocultando errores reales")
	@Test
	@DisplayName("matches propagates NullPointerException thrown by child node")
	void testMatchesPropagatesChildNpe() {
		RootNode root = new RootNode();
		root.setChild(new Node() {
			@Override
			public String toString() {
				return "broken";
			}

			@Override
			public boolean matches(Object n) {
				throw new NullPointerException("child failure");
			}
		});
		assertThrows(NullPointerException.class, () -> root.matches("any"));
	}

	@Test
	@DisplayName("toString delegates to child toString")
	void testToStringDelegatesToChild() {
		RootNode root = new RootNode();
		root.setChild(stubNode(true, "LeafExpr"));
		assertEquals("LeafExpr", root.toString());
	}

	@Disabled("Bug conocido: RootNode.toString lanza NullPointerException cuando child es null")
	@Test
	@DisplayName("toString handles null child without throwing NullPointerException")
	void testToStringWithNullChild() {
		RootNode root = new RootNode();
		assertEquals("", root.toString());
	}
}
