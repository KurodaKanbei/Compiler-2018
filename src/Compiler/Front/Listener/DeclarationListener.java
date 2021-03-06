package Compiler.Front.Listener;

import Compiler.AST.ProgramAST;
import Compiler.AST.Statement.VariableDeclarationStatement;
import Compiler.AST.Symbol.Symbol;
import Compiler.AST.Type.*;
import Compiler.Front.Parser.MxstarParser;
import Compiler.Utility.Error.CompilationError;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public class DeclarationListener extends BaseListener{
    @Override
    public void enterProgram(MxstarParser.ProgramContext ctx) {
        super.enterProgram(ctx);
    }

    @Override
    public void exitProgram(MxstarParser.ProgramContext ctx) {
        boolean findEntry = false;
        for (ParseTree x : ctx.functionDeclaration()) {
            FunctionType functionType = (FunctionType) returnNode.get(x);
            if (functionType.getOriginName().equals("main")) {
                if (!(functionType.getReturnType() instanceof IntType)) {
                    throw new CompilationError("The return type of main function is expected to be int type");
                }
                if (functionType.getParameterList().size() > 0) {
                    throw new CompilationError("The main function is expected to have no parameters");
                }
                findEntry = true;
            }
            ProgramAST.globalFunctionTable.addFunction(functionType);
        }
        if (!findEntry) {
            throw new CompilationError("Can't find main function");
        }
        for (ParseTree x : ctx.variableDeclarationStatement()) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) returnNode.get(x);
            variableDeclarationStatement.getSymbol().setGlobal();
            ProgramAST.globalVariableDeclarationStatementList.add(variableDeclarationStatement);
        }
    }

    @Override
    public void enterVariableDeclarationStatement(MxstarParser.VariableDeclarationStatementContext ctx) {
        super.enterVariableDeclarationStatement(ctx);
    }

    @Override
    public void exitVariableDeclarationStatement(MxstarParser.VariableDeclarationStatementContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        Type type = (Type) returnNode.get(ctx.type());
        VariableDeclarationStatement variableDeclarationStatement = new VariableDeclarationStatement(name, type);
        returnNode.put(ctx, variableDeclarationStatement);
    }

    @Override
    public void enterFunctionDeclaration(MxstarParser.FunctionDeclarationContext ctx) {
        super.enterFunctionDeclaration(ctx);
    }

    @Override
    public void exitFunctionDeclaration(MxstarParser.FunctionDeclarationContext ctx) {
        Type returnType = (Type) returnNode.get(ctx.getChild(0));
        String functionName = null;
        int n = ctx.type().size();
        int m = ctx.IDENTIFIER().size();
        int typeStart = 1, identifierStart = 0;
        if (returnType instanceof VoidType) {
            typeStart = 0;
        }
        if (n - typeStart < m) {
            functionName = ctx.IDENTIFIER(0).getText();
            identifierStart = 1;
        }
        List<Symbol> parameterList = new ArrayList<>();
        for (int i = 0; i < n - typeStart; i++) {
            Type parameterType = (Type) returnNode.get(ctx.type(i + typeStart));
            String parameterName = ctx.IDENTIFIER(i + identifierStart).getText();
            parameterList.add(new Symbol(parameterName, parameterType));
        }
        FunctionType functionType = new FunctionType(functionName, returnType, parameterList);
        returnNode.put(ctx, functionType);
    }

    @Override
    public void enterClassDeclaration(MxstarParser.ClassDeclarationContext ctx) {
        ClassType classType = (ClassType) returnNode.get(ctx);
        ProgramAST.symbolTable.enterScope(classType);
    }

    @Override
    public void exitClassDeclaration(MxstarParser.ClassDeclarationContext ctx) {
        ClassType classType = (ClassType) returnNode.get(ctx);
        for (ParseTree x : ctx.functionDeclaration()) {
            FunctionType functionType = (FunctionType) returnNode.get(x);
            functionType.setClassScope(classType);
            functionType.getParameterList().add(0, new Symbol("this", classType));
            if (functionType.getOriginName() == null) {
                classType.setConstructFunction(functionType);
            } else {
                classType.addMemberFunction(functionType);
            }
        }
        for (ParseTree x : ctx.variableDeclarationStatement()) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) returnNode.get(x);
            variableDeclarationStatement.setClassScope(classType);
            classType.addMemberVariable(variableDeclarationStatement);
        }
        ProgramAST.symbolTable.exitScope();
    }

    @Override
    public void exitArrayType(MxstarParser.ArrayTypeContext ctx) {
        Type type = (Type) returnNode.get(ctx.type());
        if (type instanceof ArrayType) {
            returnNode.put(ctx, new ArrayType(((ArrayType) type).getBaseType(), ((ArrayType) type).getDimension() + 1));
        } else {
            returnNode.put(ctx, new ArrayType(type, 1));
        }
    }

    @Override
    public void exitIntType(MxstarParser.IntTypeContext ctx) {
        returnNode.put(ctx, IntType.getInstance());
    }

    @Override
    public void exitStringType(MxstarParser.StringTypeContext ctx) {
        returnNode.put(ctx, StringType.getInstance());
    }

    @Override
    public void exitVoidType(MxstarParser.VoidTypeContext ctx) {
        returnNode.put(ctx, VoidType.getInstance());
    }

    @Override
    public void exitBoolType(MxstarParser.BoolTypeContext ctx) {
        returnNode.put(ctx, BoolType.getInstance());
    }

    @Override
    public void exitClassType(MxstarParser.ClassTypeContext ctx) {
        String className = ctx.IDENTIFIER().getText();
        returnNode.put(ctx, ProgramAST.classTable.getClassType(className));
    }
}
