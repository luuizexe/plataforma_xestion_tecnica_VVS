package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BinaryNode Unit Tests")
public class BinaryNodeTest {

	private static final class TestBinaryNode extends BinaryNode {
		TestBinaryNode() {
			super();
		}

		TestBinaryNode(Node child, Node secondChild) {
			super(child, secondChild);
		}

		@Override
		public String toString() {
			return "TestBinaryNode";
		}

		@Override
		public boolean matches(Object n) {
			return false;
		}
	}

	private static final class LeafNode extends Node {
		@Override
		public String toString() {
			return "Leaf";
		}

		@Override
		public boolean matches(Object n) {
			return true;
		}
	}

	@Test
	@DisplayName("Default constructor leaves first child as null")
	void testDefaultConstructorFirstChildNull() {
		BinaryNode node = new TestBinaryNode();
		assertNull(node.getChild());
	}

	@Test
	@DisplayName("Default constructor leaves second child as null")
	void testDefaultConstructorSecondChildNull() {
		BinaryNode node = new TestBinaryNode();
		assertNull(node.getSecondChild());
	}

	@Test
	@DisplayName("Two-argument constructor stores first child")
	void testTwoArgConstructorStoresFirstChild() {
		Node first = new LeafNode();
		Node second = new LeafNode();
		BinaryNode node = new TestBinaryNode(first, second);
		assertSame(first, node.getChild());
	}

	@Test
	@DisplayName("Two-argument constructor stores second child")
	void testTwoArgConstructorStoresSecondChild() {
		Node first = new LeafNode();
		Node second = new LeafNode();
		BinaryNode node = new TestBinaryNode(first, second);
		assertSame(second, node.getSecondChild());
	}

	@Test
	@DisplayName("setSecondChild updates second child")
	void testSetSecondChildUpdatesValue() {
		BinaryNode node = new TestBinaryNode();
		Node second = new LeafNode();
		node.setSecondChild(second);
		assertSame(second, node.getSecondChild());
	}

	@Test
	@DisplayName("setSecondChild allows null")
	void testSetSecondChildAllowsNull() {
		BinaryNode node = new TestBinaryNode(new LeafNode(), new LeafNode());
		node.setSecondChild(null);
		assertNull(node.getSecondChild());
	}
}
