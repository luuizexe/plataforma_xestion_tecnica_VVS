package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.FindLexer;
import gal.udc.fic.prperez.pleste.service.FindParser;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FindListener Unit Tests")
public class FindListenerTest {

	private Node parseQuery(String query) {
		FindLexer lexer = new FindLexer(CharStreams.fromString(query));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		FindParser parser = new FindParser(tokens);

		FindParser.TopContext context = parser.top();
		Node root = new RootNode();
		ParseTreeWalker.DEFAULT.walk(new FindListener(root), context);
		return root;
	}

	@Test
	@DisplayName("FindListener parses simple string test")
	void testSimpleString() {
		Node root = parseQuery("template.name ~ \"test\"");
		assertNotNull(root.getChild());
		assertTrue(root.getChild() instanceof TestString);
	}

	@Test
	@DisplayName("FindListener parses simple number test with operators")
	void testSimpleNumber() {
		Node root = parseQuery("component.field.value >= 15.5");
		assertNotNull(root.getChild());
		assertTrue(root.getChild() instanceof TestNumber);
	}

	@Test
	@DisplayName("FindListener parses test IN with list of values")
	void testSimpleIn() {
		Node root = parseQuery("user.name @ [ \"root\", \"admin\" ]");
		assertNotNull(root.getChild());
		assertTrue(root.getChild() instanceof TestIn);
	}

	@Test
	@DisplayName("FindListener parses compound expressions with AND and OR")
	void testCompoundExpression() {
		Node root = parseQuery("(template.name ~ \"A\" & template.description ~ \"B\") | template.name @ [ \"C\" ]");
		assertNotNull(root.getChild());
		assertTrue(root.getChild() instanceof OrNode);
		OrNode orNode = (OrNode) root.getChild();
		assertTrue(orNode.getChild() instanceof AndNode);
		assertTrue(orNode.getSecondChild() instanceof TestIn);
	}

	@Test
	@DisplayName("FindListener throws ParseSearchException on invalid syntax")
	void testSyntaxErrors() {
		assertThrows(ParseSearchException.class, () -> parseQuery("template.name ~ "));
		assertThrows(ParseSearchException.class, () -> parseQuery("(template.name ~ \"A\" &"));
	}
}
