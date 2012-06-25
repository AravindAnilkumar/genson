package org.genson.serialization;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.genson.Genson;
import org.genson.TransformationException;
import org.genson.annotation.JsonIgnore;
import org.genson.bean.ComplexObject;
import org.genson.bean.Primitives;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonSerializationTest {
	Genson genson = new Genson();

	@Test
	public void testJsonPrimitiveObject() throws TransformationException, IOException {
		Primitives p = createPrimitives();
		String json = genson.serialize(p);
		assertEquals(json, p.jsonString());
	}

	@Test
	public void testJsonArrayOfPrimitives() throws TransformationException, IOException {
		String expected = "[\"a\",1,3.2,null,true]";
		Object[] array = new Object[] { "a", 1, 3.2, null, true };
		String json = genson.serialize(array);
		assertEquals(json, expected);
	}

	@Test
	public void testJsonArrayOfMixedContent() throws TransformationException, IOException {
		Primitives p = createPrimitives();
		p.setIntPrimitive(-88);
		p.setDoubleObject(null);
		String expected = "[\"a\"," + p.jsonString() + ",1,3.2,null,false," + p.jsonString() + "]";
		Object[] array = new Object[] { "a", p, 1, 3.2, null, false, p };
		String json = genson.serialize(array);
		assertEquals(json, expected);
	}

	@Test
	public void testJsonComplexObject() throws TransformationException, IOException {
		Primitives p = createPrimitives();
		List<Primitives> list = Arrays.asList(p, p, p, p, p);
		ComplexObject co = new ComplexObject(p, list, list.toArray(new Primitives[list.size()]));
		String json = genson.serialize(co);
		assertEquals(json, co.jsonString());
	}

	/*
	 * Serialize all public getXX present and all the public/package fields that don't match an used
	 * XX getter.
	 */
	@Test
	public void testSerializationMixedFieldsAndGetters() throws TransformationException,
			IOException {
		String json = "{\"age\":15,\"name\":\"TOTO\",\"noField\":\"TOTO15\"}";
		ClassWithFieldsAndGetter object = new ClassWithFieldsAndGetter("TOTO", 15);
		String out = genson.serialize(object);
		assertEquals(json, out);
	}

	@Test
	public void testSerializeWithAlias() throws TransformationException, IOException {
		Genson genson = new Genson.Builder().addAlias("ClassWithFieldsAndGetter",
				ClassWithFieldsAndGetter.class).create();
		String json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
		assertTrue(json.startsWith("{\"@class\":\"ClassWithFieldsAndGetter\""));
		genson = new Genson.Builder().setWithClassMetadata(true).create();
		json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
		assertTrue(json
				.startsWith("{\"@class\":\"org.genson.serialization.JsonSerializationTest$ClassWithFieldsAndGetter\""));
	}

	private Primitives createPrimitives() {
		return new Primitives(1, new Integer(10), 1.00001, new Double(0.00001), "TEXT ...  HEY!",
				true, new Boolean(false));
	}

	@SuppressWarnings("unused")
	private static class ClassWithFieldsAndGetter {
		private final String name;
		@JsonIgnore private String lastName;
		final int age;
		public transient int skipThisField;

		public ClassWithFieldsAndGetter(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public String getNoField() {
			return name + age;
		}
	}
}