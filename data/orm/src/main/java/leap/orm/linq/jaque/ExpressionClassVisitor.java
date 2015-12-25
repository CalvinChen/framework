/*
 * Copyright TrigerSoft <kostat@trigersoft.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package leap.orm.linq.jaque;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import leap.lang.asm.AnnotationVisitor;
import leap.lang.asm.Attribute;
import leap.lang.asm.ClassReader;
import leap.lang.asm.ClassVisitor;
import leap.lang.asm.FieldVisitor;
import leap.lang.asm.MethodVisitor;
import leap.lang.asm.Opcodes;
import leap.lang.asm.Type;
import leap.orm.linq.jaque.JaqueMethodVisitor;



/**
 * Represents a visitor or rewriter for expression trees.
 * 
 * @author <a href="mailto://kostat@trigersoft.com">Konstantin Triger</a>
 */

class ExpressionClassVisitor extends ClassVisitor {

	private static final URLClassLoader lambdaLoader;
	protected ConstantExpression _me;
	protected String _method;
	protected String _methodDesc;
	protected Expression _result;
	protected Class<?> _type;
	protected Class<?>[] _argTypes;

	static {
		String folder = System
				.getProperty("jdk.internal.lambda.dumpProxyClasses");

		if (folder == null) {
			lambdaLoader = null;
		} else {
			try {
				URL[] urls = { new File(folder).toURI().toURL() };
				lambdaLoader = new URLClassLoader(urls);
			} catch (MalformedURLException mue) {
				throw new RuntimeException(mue);
			}
		}
	}

	public Expression getResult() {
		return _result;
	}

	public void setResult(Expression result) {
		_result = result;
	}

	LambdaExpression<?> lambda(Object functional) {

		if (lambdaLoader == null)
			throw new IllegalStateException(
					"Cannot load Byte Code for lambda. Ensure that 'jdk.internal.lambda.dumpProxyClasses' system setting is properly set.");

		Class<?> functionalClass = functional.getClass();

		if (!functionalClass.isSynthetic())
			throw new UnsupportedOperationException(
					"The requested object is not a Java Lambda");

		for (Method m : functionalClass.getMethods()) {
			if (!m.isDefault()) {
				_method = m.getName();
				_methodDesc = Type.getMethodDescriptor(m);
				break;
			}
		}

		_me = Expression.constant(functional, functionalClass);

		String name = functionalClass.getName();

		InputStream s = lambdaLoader.getResourceAsStream(name.substring(0,
				name.lastIndexOf('/')).replace('.', '/')
				+ ".class");

		try {
			parse(s);
		} finally {
			try {
				s.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		Expression res = getResult();
		while (res.getExpressionType() == ExpressionType.Convert)
			res = ((UnaryExpression) res).getFirst();
		InvocationExpression target = (InvocationExpression) res;

		Method actual = (Method) ((MemberExpression) target.getTarget())
				.getMember();

		ParameterExpression[] outerParams = getParams();
		Class<?> outerType = _type;

		if (!actual.isSynthetic()) {

			return Expression.lambda(outerType, target,
					Collections.unmodifiableList(Arrays.asList(outerParams)));
		}

		// TODO: in fact must recursively parse all the synthetic methods,
		// so must have a relevant visitor. and then another visitor to reduce
		// forwarded calls

		_method = actual.getName();
		_methodDesc = Type.getMethodDescriptor(actual);

		Class<?> actualClass = actual.getDeclaringClass();
		String classPath = actualClass.getName().replace('.', '/') + ".class";

		s = actualClass.getClassLoader().getResourceAsStream(classPath);

		try {
			parse(s);
		} finally {
			try {
				s.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		Expression result = TypeConverter.convert(getResult(), _type);
		ParameterExpression[] params = getParams();

		// try reduce
		List<Expression> ntArgs = target.getArguments();
		// 1. there must be enough params
		if (ntArgs.size() <= params.length) {
			boolean canReduce = true;

			// 2. newTarget must have all args as PE
			for (Expression e : ntArgs)
				if (e.getExpressionType() != ExpressionType.Parameter) {
					canReduce = false;
					break;
				}

			if (canReduce) {
				ParameterExpression[] newInnerParams = new ParameterExpression[params.length];
				for (int i = 0; i < params.length; i++)
					newInnerParams[i] = (ParameterExpression) ntArgs
							.get(params[i].getIndex());

				result = TypeConverter.convert(result, outerType);

				LambdaExpression<?> lambda = Expression.lambda(outerType,
						result, Collections.unmodifiableList(Arrays
								.asList(newInnerParams)));
				return lambda;
			}
		}

		LambdaExpression<?> inner = Expression.lambda(_type, result,
				Collections.unmodifiableList(Arrays.asList(params)));

		InvocationExpression newTarget = Expression.invoke(inner,
				target.getArguments());

		LambdaExpression<?> lambda = Expression.lambda(outerType, newTarget,
				Collections.unmodifiableList(Arrays.asList(outerParams)));

		return lambda;
	}

	protected ParameterExpression[] getParams() {
		ParameterExpression[] params = new ParameterExpression[_argTypes.length];
		for (int i = 0; i < params.length; i++)
			params[i] = Expression.parameter(_argTypes[i], i);
		return params;
	}

	protected void parse(InputStream s) {
		try {
			try {
				ClassReader reader = new ClassReader(s);
				reader.accept(this, ClassReader.SKIP_DEBUG
						| ClassReader.SKIP_FRAMES);
			} finally {
				s.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public ExpressionClassVisitor() {
		super(Opcodes.ASM5);
	}

	protected Class<?> getClass(Type t) {
		try {
			switch (t.getSort()) {
			case Type.BOOLEAN:
				return Boolean.TYPE;
			case Type.CHAR:
				return Character.TYPE;
			case Type.BYTE:
				return Byte.TYPE;
			case Type.SHORT:
				return Short.TYPE;
			case Type.INT:
				return Integer.TYPE;
			case Type.FLOAT:
				return Float.TYPE;
			case Type.LONG:
				return Long.TYPE;
			case Type.DOUBLE:
				return Double.TYPE;
			case Type.VOID:
				return Void.TYPE;
			}
			String cn = t.getInternalName();
			cn = cn != null ? cn.replace('/', '.') : t.getClassName();

			return Class.forName(cn, false, _me.getResultType()
					.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {

		// if ((access & Opcodes.ACC_SYNTHETIC) != 0)
		// return null;

		if (!_method.equals(name) || !_methodDesc.equals(desc))
			return null;

		Type ret = Type.getReturnType(desc);
		if (ret.getSort() == Type.VOID)
			throw JaqueMethodVisitor.notLambda(Opcodes.RETURN);

		_type = getClass(ret);

		Type[] args = Type.getArgumentTypes(desc);
		Class<?>[] argTypes = new Class<?>[args.length];

		for (int i = 0; i < args.length; i++)
			argTypes[i] = getClass(args[i]);

		_argTypes = argTypes;

		return new ExpressionMethodVisitor(this,
				(access & Opcodes.ACC_STATIC) == 0 ? _me : null, argTypes);
	}

	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
	}

	@Override
	public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute arg0) {
	}

	@Override
	public void visitEnd() {
	}

	@Override
	public FieldVisitor visitField(int arg0, String arg1, String arg2,
			String arg3, Object arg4) {
		return null;
	}

	@Override
	public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
	}

	@Override
	public void visitOuterClass(String arg0, String arg1, String arg2) {
	}

	@Override
	public void visitSource(String arg0, String arg1) {
	}

}
