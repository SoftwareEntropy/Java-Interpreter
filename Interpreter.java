package interpreter;
//import for Scanner, StringBuilder, Stack
import java.util.*;

public class Interpreter {
    //Hashmap to store variable and value pairings
    static HashMap<String, Double> variables = new HashMap<>();
    static boolean errorDetected = false;
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String input;
        System.out.println("Enter your statements to evaluate. Statements should end with ';'");
        System.out.println("Enter \"run\" to evalute given statements or \"exit\" to end program");
        while (!(input = sc.nextLine()).equalsIgnoreCase("run")) {
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Quitting program");
                System.exit(0);
            } else {
                check(input);
            }
        }
        sc.close();
        System.out.println("\nResults");
        System.out.println(variables);
    }
    
    //split variable and expression, perform preliminary syntax check.
    public static void check(String str) {
        String[] split = str.split("=");
        //check if split statement is not in 'var = expression' form
        if (split.length != 2) {
            System.err.println("Error: " + str + " is a unsupported statement");
            errorDetected = true;
        }
        //check if variable name begins with a digit; invalid syntax
        if (split[0].charAt(0) >= 0 && split[0].charAt(0) <= 9) {
            System.err.println("Error: " + str + " contains an invalid variable name");
            errorDetected = true;
        }
        String variableName = split[0].trim();
        //check if there are multiple variable names
        String[] var = variableName.split(" ");
        if (var.length > 1) {
            System.err.println("Error: " + str + " contains improper syntax");
            errorDetected = true;
        }
        String expression = split[1].trim();
        //check if expression ends with a semicolon; invalid syntax
        if (!expression.endsWith(";")) {
            System.err.println("Error: " + expression + " contains invalid syntax/missing semicolon");
            errorDetected = true;
        }
        if (!errorDetected) {
            //evaluate expression value, place variable name and value in hashmap
            double value = evaluateExpression(expression);
            //place variable and value in map only if no errors are detected in evaluation
            if (!errorDetected) {
                variables.put(variableName, value);
            } else {
                System.err.println("Previous entry will not be recorded");
            }
        } else {
            System.err.println("Previous entry will not be recorded");
        }
        errorDetected = false;
    }

    //evaluates the expression for various syntax errors, returns the calculated value
    public static double evaluateExpression(String str) {
        //stacks to hold operators and operands
        Stack<Double> operands = new Stack<>();
        Stack<Character> operators = new Stack<>();
        //boolean flag, set to true after a operator is read, false after an operand is read
        boolean expectOperand = true;
        //int to count number of negative sign appearances
        int negativeCount = 0;
        //loop to check every value in the expression except ending ';'
        for (int i = 0; i < str.length() - 1; i++) {
            char c = str.charAt(i);
        //if character is a digit, keep reading additional digits and build a string to resolve value
            if (Character.isDigit(c)) {
                //if digit (operand) was not expected, previous term is likely an operand as well. Syntax error
                if (!expectOperand) {
                    System.err.println("Error: Improper syntax in expression '" + str + "'");
                    errorDetected = true;
                    return 0;
                }
                StringBuilder sb = new StringBuilder();
                int numOfDecimals = 0;
                while (i < str.length() && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
                    if (str.charAt(i) == '.') {
                        numOfDecimals++;
                    }
                    sb.append(str.charAt(i));
                    i++;
                }
                //check if there are multiple decimal points; syntax error
                if (numOfDecimals > 1) {
                    System.err.println("Error: The expression \"" + str + "\" contains multiple decimal points");
                    errorDetected = true;
                    return 0;
                }
                i--;
                double value = Double.parseDouble(sb.toString());
                //reverse value if count of negative signs are odd
                if (negativeCount % 2 == 1) {
                    value = -value;
                }
                operands.push(value);
                //reset value, set operand flag to false as operands cannot follow another operand
                negativeCount = 0;
                expectOperand = false;
        //if character is a letter, it may be a variable. Build string until whitespace and resolve variable value
            } else if (Character.isLetter(c)) {
                //if variable (operand) was not expected, previous term is likely an operand as well. Syntax error
                if (!expectOperand) {
                    System.err.println("Error: Improper syntax in expression '" + str + "'");
                    errorDetected = true;
                    return 0;
                }
                StringBuilder sb = new StringBuilder();
                //if previous character is a digit; syntax error
                if ((i - 1) >= 0 && Character.isDigit(str.charAt(i-1))) {
                    System.err.println("Error: The expression \"" + str + "\" contains an invalid variable name");
                    errorDetected = true;
                    return 0;
                }
                while (i < str.length() && (Character.isLetterOrDigit(str.charAt(i)) || str.charAt(i) == '_')) {
                    sb.append(str.charAt(i));
                    i++;
                }
                i--;
                String variable = sb.toString();
                //if variable name matches a key in the hashmap, retrieve value
                if (variables.containsKey(variable)) {
                    double value = variables.get(variable);
                    //apply negative if available
                    if (negativeCount % 2 == 1) {
                        value = -value;
                    }
                    //resolve and push value to stack
                    operands.push(value);
                } else {
                    System.err.println("Error: Variable \"" + variable + "\" in the expression \"" + str + "\" is undefined.");
                    errorDetected = true;
                    return 0;
                }
                //reset value, set operand flag to false as operands cannot follow another operand
                negativeCount = 0;
                expectOperand = false;
        //if character is open parenthesis, push. Operand expected next
            } else if (c == '(') {
                //if an operator was expected, previous input was a operand, syntax error
                if (!expectOperand) {
                    System.err.println("Error: Improper syntax in expression '" + str + "'");
                    errorDetected = true;
                    return 0;
                }
                operators.push(c);
                expectOperand = true;
        //if character is close parenthesis, apply operators inside until open parenthesis reached
            } else if (c == ')') {
                //if an operand was expected, previous input was an operator, syntax error
                if (expectOperand) {
                    System.err.println("Error: Improper syntax in expression '" + str + "'");
                    errorDetected = true;
                    return 0;
                }
                while (operators.peek() != '(') {
                    char operator = operators.pop();
                    double val2 = operands.pop();
                    double val1 = operands.pop();
                    operands.push(calculate(operator, val1, val2));
                }
                //remove open parenthesis from top of stack
                operators.pop();
                //close parenthesis cannot be followed by an operand
                expectOperand = false;
        //if '-' detected, check if an operand is expected (is negative symbol), start of expression, or before open parenthsis
            } else if (c == '-' && (expectOperand || (i == 0 || str.charAt(i - 1) == '('))) {
                negativeCount++;
        //if character is an operator, proceed with checks
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                //if an operand was expected, previous input was an operator, syntax error
                if (expectOperand) {
                    System.err.println("Error: Improper syntax in expression '" + str + "'");
                    errorDetected = true;
                    return 0;
                }
                //get precedence & calculate all operators and operands on the stack with equal or higher precedence
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(c)) {
                    char operator = operators.pop();
                    double val2 = operands.pop();
                    double val1 = operands.pop();
                    operands.push(calculate(operator, val1, val2));
                }
                //push new operator to stack, operand expected to follow
                operators.push(c);
                expectOperand = true;
        //if any other characters are recorded other than blankspace, improper syntax
            } else if (c != ' ') {
                System.err.println("Error: Improper syntax in expression '" + str + "'");
                errorDetected = true;
                return 0;
            }
        }
        //end of expression, calculate all remaining operators
        while (!operators.isEmpty()) {
            char operator = operators.pop();
            double val2 = operands.pop();
            double val1 = operands.pop();
            operands.push(calculate(operator, val1, val2));
        }
        //return final calculated value
        return operands.pop();
    }

    //calculates two values given an operator. Checks for division by zero
    public static double calculate(char operator, double val1, double val2) {
        switch (operator) {
            case '*': return val1 * val2;
            case '/': if (val2 == 0) {
                System.err.println("Error: Division by 0.");
                errorDetected = true;
                return 0;
                }
                return val1 / val2;
            case '+': return val1 + val2;
            case '-': return val1 - val2;
            default: return 0;
        }
    }
    
    //determines operator precedence/priority by order of operations.
    public static int precedence(char operator) {
        switch (operator) {
            case '*':
            case '/': return 2;
            case '+':
            case '-': return 1;
            default: return 0;
        }
    }
}