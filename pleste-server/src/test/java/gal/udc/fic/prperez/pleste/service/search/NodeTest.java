package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Node Unit Tests")
public class NodeTest {

	private static final class TestNode extends Node {
		TestNode() {
			super();
		}

		TestNode(Node child) {
			super(child);
		}

		@Override
		public String toString() {
			return "TestNode";
		}

		@Override
		public boolean matches(Object n) {
			return true;
		}
	}

	@Test
	@DisplayName("Default constructor initializes child as null")
	void testDefaultConstructorLeavesChildNull() {
		Node node = new TestNode();
		assertNull(node.getChild());
	}

	@Test
	@DisplayName("Single-argument constructor stores child")
	void testConstructorWithChildStoresChild() {
		Node child = new TestNode();
		Node parent = new TestNode(child);
		assertSame(child, parent.getChild());
	}

	@Test
	@DisplayName("setChild updates stored child")
	void testSetChildUpdatesChild() {
		Node parent = new TestNode();
		Node child = new TestNode();
		parent.setChild(child);
		assertSame(child, parent.getChild());
	}

	@Test
	@DisplayName("setChild allows clearing child with null")
	void testSetChildAllowsNull() {
		Node parent = new TestNode(new TestNode());
		parent.setChild(null);
		assertNull(parent.getChild());
	}
}
