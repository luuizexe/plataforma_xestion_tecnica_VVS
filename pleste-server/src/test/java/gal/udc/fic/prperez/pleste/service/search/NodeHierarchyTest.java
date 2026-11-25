package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NodeHierarchy Unit Tests")
public class NodeHierarchyTest {

	private static class DummyNode extends Node {
		private final boolean matchResult;

		public DummyNode(boolean matchResult) {
			super();
			this.matchResult = matchResult;
		}

		public DummyNode(Node child, boolean matchResult) {
			super(child);
			this.matchResult = matchResult;
		}

		@Override
		public String toString() {
			return "Dummy(" + matchResult + ")";
		}

		@Override
		public boolean matches(Object n) {
			return matchResult;
		}
	}

	@Test
	@DisplayName("Node constructors, child getter and setter")
	void testNodeBasics() {
		DummyNode leaf = new DummyNode(true);
		DummyNode parent = new DummyNode(leaf, true);
		assertSame(leaf, parent.getChild());

		DummyNode anotherLeaf = new DummyNode(false);
		parent.setChild(anotherLeaf);
		assertSame(anotherLeaf, parent.getChild());
	}

	@Test
	@DisplayName("BinaryNode constructors, children getters and setters")
	void testBinaryNodeBasics() {
		DummyNode c1 = new DummyNode(true);
		DummyNode c2 = new DummyNode(false);

		BinaryNode andNode = new AndNode();
		assertNull(andNode.getChild());
		assertNull(andNode.getSecondChild());

		andNode.setChild(c1);
		andNode.setSecondChild(c2);
		assertSame(c1, andNode.getChild());
		assertSame(c2, andNode.getSecondChild());

		// Test constructor with two children
		BinaryNode orNode = new OrNode();
		orNode.setChild(c1);
		orNode.setSecondChild(c2);
		assertSame(c1, orNode.getChild());
		assertSame(c2, orNode.getSecondChild());

		// Test BinaryNode 2-arg constructor via an anonymous subclass
		BinaryNode twoArgNode = new BinaryNode(c1, c2) {
			@Override
			public String toString() {
				return "twoArg";
			}

			@Override
			public boolean matches(Object n) {
				return true;
			}
		};
		assertSame(c1, twoArgNode.getChild());
		assertSame(c2, twoArgNode.getSecondChild());
	}

	@Test
	@DisplayName("RootNode matches and toString")
	void testRootNode() {
		RootNode root = new RootNode();
		// Child is null -> NullPointerException caught and returns false
		assertFalse(root.matches("anyObject"));

		DummyNode child = new DummyNode(true);
		root.setChild(child);
		assertTrue(root.matches("anyObject"));
		assertEquals("Dummy(true)", root.toString());

		DummyNode falseChild = new DummyNode(false);
		root.setChild(falseChild);
		assertFalse(root.matches("anyObject"));
	}

	@Test
	@DisplayName("AndNode logic matches and toString")
	void testAndNode() {
		AndNode andNode = new AndNode();
		DummyNode t = new DummyNode(true);
		DummyNode f = new DummyNode(false);

		andNode.setChild(t);
		andNode.setSecondChild(t);
		assertTrue(andNode.matches("test"));
		assertEquals("(Dummy(true) AND Dummy(true))", andNode.toString());

		andNode.setChild(t);
		andNode.setSecondChild(f);
		assertFalse(andNode.matches("test"));

		andNode.setChild(f);
		andNode.setSecondChild(t);
		assertFalse(andNode.matches("test"));

		andNode.setChild(f);
		andNode.setSecondChild(f);
		assertFalse(andNode.matches("test"));
	}

	@Test
	@DisplayName("OrNode logic matches and toString")
	void testOrNode() {
		OrNode orNode = new OrNode();
		DummyNode t = new DummyNode(true);
		DummyNode f = new DummyNode(false);

		orNode.setChild(t);
		orNode.setSecondChild(t);
		assertTrue(orNode.matches("test"));
		assertEquals("(Dummy(true) OR Dummy(true))", orNode.toString());

		orNode.setChild(t);
		orNode.setSecondChild(f);
		assertTrue(orNode.matches("test"));

		orNode.setChild(f);
		orNode.setSecondChild(t);
		assertTrue(orNode.matches("test"));

		orNode.setChild(f);
		orNode.setSecondChild(f);
		assertFalse(orNode.matches("test"));
	}
}
