package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Yahoo", "thelegend27@yahoo.com", true),//me
                Arguments.of("Two Letters", "..@gmail.com", true),//me
                Arguments.of("Two Letters", "..@gmail.com", true),//me

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Too Short", "a@gmail.com", false),//me
                Arguments.of("Spaces", "thelegend 27@gmail.com", false),//me
                Arguments.of("Missing Com", "thelegend27@gmail.", false),//me
                Arguments.of("No Gmail", "thelegend27@.com", false),//me
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "I<3pancakes13", true),
                Arguments.of("15 Characters With NewLine", "I<3pancakes\n555", true),//me
                Arguments.of("17 Characters With Tab", "1234567890aA bB\t!", true),//me
                Arguments.of("19 Characters With Spaces", "1234567890aA bB cC!", true),//me

                Arguments.of("5 Characters", "5five", false),
                Arguments.of("9 Characters", "123456789", false),//me
                Arguments.of("10 Characters", "1234567890", false),//me
                Arguments.of("21 Characters", "1234567890abcdefghijk", false),//me
                Arguments.of("14 Characters", "i<3pancakes14!", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Escape Tabs", "['\n','\t','c']", true),//me
                Arguments.of("Front Spaces", "[ 'b', 'a', 'c']", true),//me
                Arguments.of("Behind Spaces", "['b' ,'a' ,'c' ]", true),//me
                Arguments.of("Empty Brackets", "[]", true),//me

                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Character", "['','','']", false),//me
                Arguments.of("Too Many Character", "['aa','bb','cc']", false),//me
                Arguments.of("Missing Single Quote", "[a,b,c]", false),//me
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);//TODO
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Multiple Digit Decimal", "10100.001", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Positive Decimal", "1.0", true),//me
                Arguments.of("All Numbers", "123456789.0", true),//me
                Arguments.of("Leading 0", "0.0", true),//me

                Arguments.of("Integer Only", "1", false),
                Arguments.of("Double Negative Decimal", "--1.0", false),//me
                Arguments.of("Interger and Decimal no Digit", "1.", false),//me
                Arguments.of("Leading 0", "01.0", false),//me
                Arguments.of("Too Many Decimals", "1.0.0", false),//me
                Arguments.of("Leading Decimal", ".5", false)
        ); //TODO
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);//TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Hello World", "\"Hello, World!\"", true),
                Arguments.of("New Line", "\"1\\n2\"", true),
                Arguments.of("Escape", "\"1\\t2\"", true),
                Arguments.of("Escape Multiple", "\"1\\t2\\n3\"", true),
                Arguments.of("Quote escaped", "\"1\\\"2\\n3\"", true),

                Arguments.of("Solo Backslash", "\"\\\"", false),
                Arguments.of("Only escape char", "\"\n\"", false),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Quote in string", "\"unterm\"inated\"", false),
                Arguments.of("Invalid Escape", "\"\\q\"", false)
        ); //TODO
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
