import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompilationEngine {
	private JackTokenizer jt;
	private FileWriter fw 			= null;
	private int indent 				= 0;		// xml output indentation
	private boolean debug			= true;		// turns on console debugging output
	
	private final String ANSI_RESET 	= "\u001B[0m";	// console terminal color
	private final String ANSI_RED 		= "\u001B[31m";
	private final String ANSI_GREEN 	= "\u001B[32m";
	private final String ANSI_YELLOW 	= "\u001B[33m";
	private final String ANSI_BLUE 		= "\u001B[34m";
	private final String ANSI_CYAN 		= "\u001B[36m";
	
	/** Creates a new compilation engine with the given input and output.
	 *  THe next routing called must be compileClass.
	 */
	public CompilationEngine(String path) {
		
		jt = new JackTokenizer(path);
		
		File fl = new File(path.substring(0, path.lastIndexOf(".")) + ".xml");
		
		try {
			fw = new FileWriter(fl);
			
			if ( jt.hasMoreTokens() )
				jt.advance();
			
			compileClass();
			
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Compiles a complete class.
	 *  Syntax: 'class' className '{' classVarDec* subroutineDec* '}'
	 */
	public void compileClass() {
		writeToken("<class>");
		indent++;
		
		writeTokenAdv("class");		// class
		
		writeTokenAdv("-i");		// className
		
		writeTokenAdv("\\{");		// {
		
		while ( tokenIs("static|field") )
			compileClassVarDec();
		
		while ( tokenIs("constructor|method|function") )
			compileSubroutine();
		
		writeTokenAdv("\\}");		// }
		
		indent--;
		writeToken("</class>");
	}
	
	/** Compiles a static declaration or a field declaration.
	 *  Process additional tokens until it encounters a symbol ';'.
	 *  Then advances over to next token.
	 *  Syntax: field|static keyword|identifier identifier ;
	 *  Keyword must be either int, boolean or char.
	 *  Syntax: ('static'|'field') type varName (',' varName)* ';'
	 */
	public void compileClassVarDec() {
		
		writeLine("<classVarDec>");
		indent++;
		
		writeTokenAdv("static|field");
			
		compileDec();
		
		indent--;
		writeLine("</classVarDec>");
	}
	
	/** Compiles a var declaration.
	 *  Syntax: 'var' type varName (',' varName)* ';'
	 */
	public void compileVarDec() {
		
		writeLine("<varDec>");
		indent++;
		
		writeTokenAdv("var");
			
		compileDec();
		
		indent--;
		writeLine("</varDec>");
	}
	
	/** Compiles a local or class var declaration statement
	 *  Used by compileClassVarDec and compileVarDec
	 *  Syntax: type varName (',' varName)* ';'
	 */
	private void compileDec() {
		if ( isaType2() )
			writeTokenAdv(null);		//  type
		
		writeTokenAdv("-i");			// varName
		
		while ( tokenIs("\\,") ) {
			writeTokenAdv("\\,");		// ,
			writeTokenAdv("-i");		// varName
		}
	
		writeTokenAdv("\\;");	// ;
	}
	
	/** Compiles a complete method, function, or constructor.
	 *  Syntax: (constructor|function|method)  (void|type) subroutineName ( parameterList ) subroutineBody
	 *  e.g. constructor Square new(int x, int y, int size) { statements; }
	 */
	public void compileSubroutine() {
		
		writeLine("<subroutineDec>");
		indent++;
		
		writeTokenAdv("constructor|function|method");
		
		writeTokenAdv("-t"); 	// type
		
		writeTokenAdv("-i");	// subroutineName
		
		writeTokenAdv("\\(");		// (
		
		compileParameterList();
		
		writeTokenAdv("\\)");		// )
		
		compileSubroutineBody();
		
		indent--;
		writeLine("</subroutineDec>");
	}
	
	/** Compiles a (possibly empty) parameter list, not including the enclosing "()".
	 *  syntax: ( (type varName) ( ',' type varName)* )?
	 *  type: keyword is (char|int|boolean), identifier is built-in class type
	 */
	public void compileParameterList() {
		
		writeLine("<parameterList>");
		indent++;
		
		if (! tokenIs("\\)") ) {
			writeTokenAdv("-t2");	// type
			
			writeTokenAdv("-i");	// varName
			
			while ( tokenIs("\\,") ) {
				
				writeTokenAdv("\\,");	// ,
				
				writeTokenAdv("-t2");	// type
				
				writeTokenAdv("-i");	// varName
			}
		}
		
		indent--;
		writeLine("</parameterList>");
	}
	
	/** Compiles a subroutine body, including { }.
	 *  Syntax: '{' (varDec)* statements '}'
	 *  Keeps track of no. of opening and closing braces.
	 */
	private void compileSubroutineBody() {
		
		writeLine("<subroutineBody>");
		indent++;
		
		writeTokenAdv("\\{");
		
		while ( tokenIs("var") )
			compileVarDec();
		
		compileStatements();
		
		writeTokenAdv("\\}");
		
		indent--;
		writeLine("</subroutineBody>");
	}
	
	/** Compiles a sequence of statements, not
	 *  including the enclosing "{}".
	 *  Syntax: statement*
	 *  Statement prefix: let | if | while | do | return
	 */
	public void compileStatements() {
		
		writeLine("<statements>");
		indent++;
		
		while ( tokenIs("let|do|while|if|return") ) {
			switch(jt.keyWord()) {
			
			case JackTokenizer.DO :
				compileDo();
				break;
				
			case JackTokenizer.LET :
				compileLet();
				break;
				
			case JackTokenizer.WHILE :
				compileWhile();
				break;
				
			case JackTokenizer.RETURN :
				compileReturn();
				break;
				
			case JackTokenizer.IF :
				compileIf();
				break;
				
			default:
				break;
			}
		}
		
		indent--;
		writeLine("</statements>");
	}
	
	/** Compiles a do statement.
	 *  Syntax: 'do' subroutineCall ';'
	 */
	public void compileDo() {
		
		writeLine("<doStatement>");
		indent++;
		
		writeTokenAdv("do");
		
		compileSubroutineCall();
		
		writeTokenAdv("\\;");
		
		indent--;
		writeLine("</doStatement>");
	}
	
	/** Compiles a subroutine call expression
	 *  Syntax: subroutineName '(' expressionList ')' | 
	 *              (className | varName) '.' subroutineName '(' expressionList ')'
	 */	
	private void compileSubroutineCall() {
		
		writeTokenAdv("-i");	// subroutineName or (className or varName)
		
		if (tokenIs("\\(") ) {
			
			writeTokenAdv("\\(");	// (
			
			compileExpressionList();
			
			writeTokenAdv("\\)");	// )
		}
		else if (tokenIs("\\.") ) {
			
			writeTokenAdv("\\.");	// .
			
			writeTokenAdv("-t");	// subroutineName
			
			writeTokenAdv("\\(");	// (
			
			compileExpressionList();
			
			writeTokenAdv("\\)");	// )
		}
	}
	
	/** Compiles a let statement.
	 *  Syntax: 'let' varName ( '[' expression ']' )? '=' expression ';'
	 */
	public void compileLet() {
		
		writeLine("<letStatement>");
		indent++;
		
		writeTokenAdv("let");		// let
		
		writeTokenAdv("-i");		// varName
		
		if ( tokenIs("\\[") ) {
			
			writeTokenAdv("\\[");	// [
			
			compileExpression();
			
			writeTokenAdv("\\]");	// ]
		}
			
		writeTokenAdv("\\=");		// =
			
		compileExpression();
			
		writeTokenAdv("\\;");		// ;
		
		indent--;
		writeLine("</letStatement>");
	}
	
	/** Compiles a while statement.
	 *  Syntax: while ( expression ) { statements }
	 */
	public void compileWhile() {
		
		writeLine("<whileStatement>");
		indent++;
		
		writeTokenAdv("while");		// while
		
		writeTokenAdv("\\(");		// (
		
		compileExpression();
		
		writeTokenAdv("\\)");		// )
		
		writeTokenAdv("\\{");		// {
		
		compileStatements();
		
		writeTokenAdv("\\}");		// }
		
		indent--;
		writeLine("</whileStatement>");
	}
	
	/** Compiles a return statement.
	 *  Syntax: 'return' expression? ';'
	 *  return statement can occur anywhere, not necessary the last statement in subroutine body
	 */
	public void compileReturn() {
		
		writeLine("<returnStatement>");
		indent++;
		
		writeTokenAdv("return");	// return
		
		if (! tokenIs("\\;") )
			compileExpression();
		
		writeTokenAdv("\\;");		// ;
		
		indent--;
		writeLine("</returnStatement>");
	}
		
	/** Compiles a if statement,
	 *  possibly with a trailing else clause.
	 *  Syntax: 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements '}' )?
	 */
	public void compileIf() {
		
		writeLine("<ifStatement>");
		indent++;
		
		writeTokenAdv("if");		// if
		
		writeTokenAdv("\\(");		// (
		
		compileExpression();
		
		writeTokenAdv("\\)");		// )
		
		writeTokenAdv("\\{");		// {
		
		compileStatements();
		
		writeTokenAdv("\\}");		// }
		
		if ( tokenIs("else") ) {
			
			writeTokenAdv("else");	// else
			
			writeTokenAdv("\\{");	// {
			
			compileStatements();
			
			writeTokenAdv("\\}");	// }
		}
		
		indent--;
		writeLine("</ifStatement>");
	}
	
	/** Compiles a (possibly empty) comma-separated list of expressions.
	 *  Syntax: ( expression (',' expression)* )?
	 *  Called by subroutineCall()
	 */
	public void compileExpressionList() {
		
		writeLine("<expressionList>");
		indent++;
		
		if (! tokenIs("\\)") ) {
			compileExpression();
			
			while ( tokenIs("\\,")) {
				
				writeTokenAdv("\\,");		// ,
				
				compileExpression();
			}
		}
		
		indent--;
		writeLine("</expressionList>");
	}
	
	/** Compiles an expression.
	 *  Syntax: term (op term)*
	 */
	public void compileExpression() {
		
		writeLine("<expression>");
		indent++;
		
		compileTerm();
		
		while ( isOp() ) {
			writeTokenAdv(null);	// op
			
			compileTerm();
		}
		
		indent--;
		writeLine("</expression>");
	}
	
	/** Checks whether the operator is valid
	 * @return boolean
	 */
	private boolean isOp() {
		String sym   = null;
		String opStr = "+-*/&|<>=";
		
		if ( ! (jt.tokenType() == JackTokenizer.SYMBOL))
			return false;
		
		sym = jt.symbol() + "";
		
		if (opStr.contains(sym))
			return true;
		else
			return false;
	}
	
	/** Compiles a term. This routine is faced with a slight difficulty 
	 *  when trying to decide between some of the alternative parsing rules.
	 *  Specifically, if the current token is an identifier, the routine must
	 *  distinguish between a variable, an array entry, and a subroutine call.
	 *  A single look-ahead token, which may be one of "[", "(", or "." suffices
	 *  to distinguish between the three possibilities. Any other token is not part
	 *  of this term and should not be advanced over.
	 *  Syntax: integerConstant | stringConstant | keywordConstant | varName |
	 *              varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term
	 *
	 *  Syntax of subroutineCall: subroutineName '(' expressionList ')' | 
	 *              (className | varName) '.' subroutineName '(' expressionList ')'
	 *   
	 *  varName requires look ahead but will not consume additional token if it is just a simple term.
	 */
	public void compileTerm() {
		
		boolean isTerm = true;
		
		writeLine("<term>");
		indent++;
		
		switch (jt.tokenType()) { 
		
		case JackTokenizer.INT_CONST:
		case JackTokenizer.STRING_CONST:
			writeTokenAdv(null);
			break;
			
		case JackTokenizer.KEYWORD:
			if ( tokenIs("true|false|null|this"))
				writeTokenAdv(null);
			break;
				
		case JackTokenizer.IDENTIFIER:
			compileTermLookAhead();
			break;
			
		case JackTokenizer.SYMBOL:		// ( or unary op
			processSym();
			break;
		
		default:
			break;
		}
		
		indent--;
		writeLine("</term>");
	}
	
	/** processes the identifier and look ahead 1 token
	 *  Uses peekToken() to peek at next token; it does not advance to next token.
	 */
	private void compileTermLookAhead() {
		
		String nextToken = getNextTokenVal();		// look ahead 1 token
		
		if ( nextToken.equals("[") )
			compileArray();
		
		else if ( nextToken.equals("(") || nextToken.equals(".") )
			compileSubroutineCall();
		
		else
			writeTokenAdv("-i");	// varName
		
	};
	
	/** compiles array term of the form varName[expression]
	 */
	private void compileArray() {
		
		writeTokenAdv("-i");	// identifier
		
		writeTokenAdv("\\[");	// [
		
		compileExpression();
		 
		writeTokenAdv("\\]");	// ]
	}
	
	/** processes either the format: '(' expression ')' or ('~'|'-') Term
	 */
	private void processSym() {
		
		if ( tokenIs("\\(")) {
			
			writeTokenAdv("\\(");	// (
			
			compileExpression();
			
			writeTokenAdv("\\)");	// )
		}
		else if ( tokenIs("\\~|\\-")) {
			
			writeTokenAdv("\\~|\\-");	// ~ or -
			
			compileTerm();
		}
	}
	
	/** Is a type: int, char, boolean, void, built-in className
	 * @return boolean
	 */
	private boolean isaType() {
		if (tokenIs("int|char|boolean|void") || jt.tokenType() == JackTokenizer.IDENTIFIER)
			return true;
		return false;
	}
	
	/** Is a type: int, char, boolean, built-in className. Exclude void
	 * @return boolean
	 */
	private boolean isaType2() {
		if (tokenIs("int|char|boolean") || jt.tokenType() == JackTokenizer.IDENTIFIER)
			return true;
		return false;
	}
	
	/** Gets current calling method's name. For debugging
	 * @return String
	 */
	private static String getCurrentMethodName(int t) {
	    return Thread.currentThread().getStackTrace()[t].getMethodName();
	}
	
	/** Gets line no. in the token list file filenameT.xml
	 * @return int
	 */
	private int lineNo() {
		return (jt.getTokenPos() + 1);
	}
	
	/** Writes indentation at beginning of line
	 * @return string
	 */
	private String indents() {
		return new String(new char[indent]).replace("\0", "  ");
	}
	
	/** Writes indentation at beginning of line for debugging output
	 * @return string
	 */
	private String indentD() {
		return ANSI_BLUE + new String(new char[indent]).replace("\0", "| ") + ANSI_RESET;
	}
	
	/** Token val is a ?
	 * @return boolean
	 */
	private boolean tokenIs(String s) {
		if ( getTokenVal().matches(s) )
			return true;
		return false;
	}
	
	/** Returns the value of given token
	 * @return string
	 */
	private String getTokenVal() {
		return jt.xmlToString(jt.getToken()).trim();
	}
	
	/** Returns the value of token at one position ahead.
	 * @return string
	 */
	private String getNextTokenVal() {
		return jt.xmlToString(jt.peekToken(1)).trim();
	}
	
	/** Prints value of current token. For debugging
	 * @return string
	 */
	private void printCurrentTokenVal() {
		if (debug)
			System.out.format("%s%s%d. Info  %s : %s%s%s\n", 
				indentD(), ANSI_GREEN, lineNo(), 
				getCurrentMethodName(4), ANSI_YELLOW, getTokenVal(),
				ANSI_RESET);
	}
	
	private void printErrMsg(String s) {
		System.out.format("%s%s%d. Error %s : Expected %s but got a %s.%s\n",
				indents(), ANSI_CYAN, lineNo(), getCurrentMethodName(4), 
				jt.getToken(), s, ANSI_RESET);
	}
	
	/** Writes token and advances to next token
	 */
	private void writeTokenAdv(String s) {
		if (s != null) {
			if (s.equals("-i")) {
				if (! (jt.tokenType() == JackTokenizer.IDENTIFIER)) {
					printErrMsg(s);
					return;
				}
			}
			else if (s.equals("-t")) {
				if (! isaType()) {
					printErrMsg(s);
					return;
				}
			}
			else if (s.equals("-t2")) {
				if (! isaType2()) {
					printErrMsg(s);
					return;
				}	
			}
			else if (! tokenIs(s)) {
				printErrMsg(s);
				return;
			}
		}
			
		writeToken();
		jt.advance();
	}
	
	/** Writes a line to output file
	 */
	private void writeLine(String s) {
		try {
			fw.write(indents() + s + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (debug)
			printCurrentTokenVal();
	}
	
	/** Writes the token in xml format to output file
	 */
	private void writeToken() {
		try {
			fw.write(indents() + jt.getToken() + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Writes the token given in the param to output file
	 *  Used for look ahead cases
	 */
	private void writeToken(String s) {
		String str = getTokenVal();
		
		try {
			fw.write(indents() + s + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
