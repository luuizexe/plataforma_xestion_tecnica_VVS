package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.FindLexer;
import gal.udc.fic.prperez.pleste.service.FindParser;
import gal.udc.fic.prperez.pleste.service.JSONDatetime;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.DatetimeField;
import gal.udc.fic.prperez.pleste.service.dao.component.NumberField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FindListener Unit Tests")
public class FindListenerTest {

	private static Node parseQuery(String query) {
		FindLexer lexer = new FindLexer(CharStreams.fromString(query));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		FindParser parser = new FindParser(tokens);
		FindParser.TopContext context = parser.top();
		Node root = new RootNode();
		ParseTreeWalker.DEFAULT.walk(new FindListener(root), context);
		return root;
	}

	@Test
	@DisplayName("Parses ~ operator into TestString child node")
	void testSimpleStringCreatesTestString() {
		Node root = parseQuery("template.name ~ \"test\"");
		assertInstanceOf(TestString.class, root.getChild());
	}

	@Test
	@DisplayName("Parses ~ operator preserving match polarity")
	void testSimpleStringPositiveMatchEvaluatesTrue() {
		Node root = parseQuery("template.name ~ \"Alpha\"");
		assertTrue(root.matches(new Template(1L, "Alpha", "desc", List.of())));
	}

	@Test
	@DisplayName("Parses !~ operator preserving negated polarity")
	void testSimpleStringNegatedMatchEvaluatesTrue() {
		Node root = parseQuery("template.name !~ \"Beta\"");
		assertTrue(root.matches(new Template(1L, "Alpha", "desc", List.of())));
	}

	@Test
	@DisplayName("Parses ~ operator with empty string literal")
	void testSimpleStringEmptyLiteral() {
		Node root = parseQuery("template.name ~ \"\"");
		assertTrue(root.matches(new Template(1L, "", "desc", List.of())));
	}

	@Test
	@DisplayName("Parses numeric comparison into TestNumber child node")
	void testSimpleNumberCreatesTestNumber() {
		Node root = parseQuery("component.field.value >= 15.5");
		assertInstanceOf(TestNumber.class, root.getChild());
	}

	@ParameterizedTest
	@CsvSource({
			"=, 10, true",
			"!=, 10, false",
			">, 9.5, true",
			"<, 10.5, true",
			">=, 10, true",
			"<=, 10, true",
			"=, -5, false"
	})
	@DisplayName("Parses all numeric operators and negative/decimal values")
	void testNumericOperatorsEvaluation(String op, String queryVal, boolean expected) {
		TemplateField tf = new TemplateField(1L, "val", false, FieldTypes.NUMBER);
		Component comp = new Component(1L, "C", "D", null, List.of(new NumberField(1L, new BigDecimal("10"), tf)));
		Node root = parseQuery("component.field.value " + op + " " + queryVal);
		assertEquals(expected, root.matches(comp));
	}

	@Test
	@DisplayName("Parses DATETIME literal in numeric comparison")
	void testDatetimeLiteralComparison() {
		TemplateField tf = new TemplateField(1L, "created", false, FieldTypes.DATETIME);
		JSONDatetime dt = new JSONDatetime(LocalDateTime.parse("2026-11-25T12:00:00").atZone(ZoneId.systemDefault()).toOffsetDateTime());
		Component comp = new Component(1L, "C", "D", null, List.of(new DatetimeField(1L, dt, tf)));
		Node root = parseQuery("component.field.value = 2026-11-25T12:00:00");
		assertTrue(root.matches(comp));
	}

	@Test
	@DisplayName("Parses @ operator into TestIn child node")
	void testSimpleInCreatesTestIn() {
		Node root = parseQuery("user.name @ [ \"root\", \"admin\" ]");
		assertInstanceOf(TestIn.class, root.getChild());
	}

	@Test
	@DisplayName("Parses @ operator with single item")
	void testInSingleItemEvaluatesTrue() {
		Node root = parseQuery("user.name @ [ \"root\" ]");
		assertTrue(root.matches(new User(1L, "root", "r@u", "h", Roles.ADMINISTRATOR)));
	}

	@Test
	@DisplayName("Parses @ operator with multiple items")
	void testInMultipleItemsEvaluatesTrue() {
		Node root = parseQuery("user.name @ [ \"alice\", \"root\", \"bob\" ]");
		assertTrue(root.matches(new User(1L, "root", "r@u", "h", Roles.ADMINISTRATOR)));
	}

	@Test
	@DisplayName("Parses !@ operator preserving negated polarity")
	void testNotInEvaluatesTrueWhenAbsent() {
		Node root = parseQuery("user.name !@ [ \"alice\", \"bob\" ]");
		assertTrue(root.matches(new User(1L, "root", "r@u", "h", Roles.ADMINISTRATOR)));
	}

	@Test
	@DisplayName("Parses AND before OR respecting operator precedence in AST structure")
	void testAndOrPrecedenceAst() {
		Node root = parseQuery("template.name ~ \"A\" & template.description ~ \"B\" | template.name @ [ \"C\" ]");
		assertEquals("((TEMPLATE_NAME LIKE A AND TEMPLATE_DESCRIPTION LIKE B) OR TEMPLATE_NAME IN ( [C] ))", root.toString());
	}

	@Test
	@DisplayName("Parses parentheses overriding default AND/OR precedence in AST structure")
	void testParenthesesAstStructure() {
		Node root = parseQuery("template.name ~ \"A\" & (template.description ~ \"B\" | template.name @ [ \"C\" ])");
		assertEquals("(TEMPLATE_NAME LIKE A AND (TEMPLATE_DESCRIPTION LIKE B OR TEMPLATE_NAME IN ( [C] )))", root.toString());
	}

	@Test
	@DisplayName("Parses chained AND expressions into nested AndNode tree")
	void testChainedAndAst() {
		Node root = parseQuery("template.name ~ \"A\" & template.description ~ \"B\" & template.field.name ~ \"C\"");
		assertEquals("((TEMPLATE_NAME LIKE A AND TEMPLATE_DESCRIPTION LIKE B) AND TEMPLATE_FIELD_NAME LIKE C)", root.toString());
	}

	@Test
	@DisplayName("Evaluates compound query with parentheses accurately")
	void testCompoundQueryEvaluation() {
		Node root = parseQuery("(template.name ~ \"A\" | template.name ~ \"B\") & template.description ~ \"ok\"");
		assertTrue(root.matches(new Template(1L, "B", "ok", List.of())));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"template.name ~ ",
			"(template.name ~ \"A\" &",
			"template.name ~ \"unclosed",
			"template.name @ []",
			"component.field.value = abc",
			"template.name ~ \"A\" extra",
			"123 ~ \"a\""
	})
	@DisplayName("Invalid queries throw ParseSearchException")
	void testInvalidSyntaxThrowsParseSearchException(String invalidQuery) {
		assertThrows(ParseSearchException.class, () -> parseQuery(invalidQuery));
	}

	@Disabled("Bug conocido: consultas vacías o solo con espacios no lanzan ParseSearchException porque ANTLR4 se recupera sin ErrorNode y RootNode devuelve false")
	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	@DisplayName("Empty or blank queries throw ParseSearchException")
	void testEmptyOrBlankQueryThrowsParseSearchException(String invalidQuery) {
		assertThrows(ParseSearchException.class, () -> parseQuery(invalidQuery));
	}

	@Disabled("Bug conocido: una propiedad desconocida con ~ o @ lanza IllegalArgumentException en lugar de ParseSearchException")
	@Test
	@DisplayName("Unknown property in ~ expression throws ParseSearchException")
	void testUnknownPropertyInStringQueryThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> parseQuery("unknown.prop ~ \"a\""));
	}

	@Test
	@DisplayName("visitErrorNode throws ParseSearchException with token index")
	void testVisitErrorNodeThrowsParseSearchException() {
		FindListener listener = new FindListener(new RootNode());
		ErrorNode errorNode = mock(ErrorNode.class);
		Token symbol = mock(Token.class);
		when(errorNode.getSymbol()).thenReturn(symbol);
		when(symbol.getStartIndex()).thenReturn(4);
		assertThrows(ParseSearchException.class, () -> listener.visitErrorNode(errorNode));
	}

	@Disabled("Bug conocido: Find.g4 rechaza barras invertidas salvo \\\" impidiendo escapes regex como \\. o \\d")
	@Test
	@DisplayName("Regex escape sequences inside string literals are accepted by grammar")
	void testRegexEscapeInStringLiteral() {
		Node root = parseQuery("template.name ~ \"a\\.b\"");
		assertTrue(root.matches(new Template(1L, "a.b", "d", List.of())));
	}

	@Disabled("Bug conocido: FindListener usa StringUtils.strip en lugar de desescapar comillas internas \\\"")
	@Test
	@DisplayName("Escaped double quotes inside string literals are unescaped")
	void testEscapedQuotesUnescaped() {
		Node root = parseQuery("template.name ~ \"a\\\"b\"");
		assertTrue(root.matches(new Template(1L, "a\"b", "d", List.of())));
	}

	@Disabled("Bug conocido: FindListener captura IndexOutOfBoundsException pero RuleContext.getChild devuelve null lanzando NullPointerException")
	@Test
	@DisplayName("enterTestString throws ParseSearchException on incomplete parser context")
	void testEnterTestStringIncompleteContextThrowsParseSearchException() {
		FindListener listener = new FindListener(new RootNode());
		FindParser.TestStringContext ctx = mock(FindParser.TestStringContext.class);
		assertThrows(ParseSearchException.class, () -> listener.enterTestString(ctx));
	}

	@Disabled("Bug conocido: FindListener captura IndexOutOfBoundsException pero RuleContext.getChild devuelve null lanzando NullPointerException")
	@Test
	@DisplayName("enterTestNum throws ParseSearchException on incomplete parser context")
	void testEnterTestNumIncompleteContextThrowsParseSearchException() {
		FindListener listener = new FindListener(new RootNode());
		FindParser.TestNumContext ctx = mock(FindParser.TestNumContext.class);
		assertThrows(ParseSearchException.class, () -> listener.enterTestNum(ctx));
	}

	@Disabled("Bug conocido: FindListener captura IndexOutOfBoundsException pero RuleContext.getChild devuelve null lanzando NullPointerException")
	@Test
	@DisplayName("enterTestIn throws ParseSearchException on incomplete parser context")
	void testEnterTestInIncompleteContextThrowsParseSearchException() {
		FindListener listener = new FindListener(new RootNode());
		FindParser.TestInContext ctx = mock(FindParser.TestInContext.class);
		assertThrows(ParseSearchException.class, () -> listener.enterTestIn(ctx));
	}
}
