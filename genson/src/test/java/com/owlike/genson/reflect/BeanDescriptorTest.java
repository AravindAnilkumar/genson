package com.owlike.genson.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.GenericArrayType;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.owlike.genson.GensonBuilder;
import org.junit.Test;

import com.owlike.genson.Converter;
import com.owlike.genson.Factory;
import com.owlike.genson.Genson;
import com.owlike.genson.annotation.JsonCreator;
import com.owlike.genson.annotation.JsonIgnore;
import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.convert.BasicConvertersFactory;
import com.owlike.genson.convert.DefaultConverters;
import com.owlike.genson.convert.DefaultConverters.CollectionConverter;
import com.owlike.genson.reflect.AbstractBeanDescriptorProvider.ContextualConverterFactory;

import static org.junit.Assert.*;

public class BeanDescriptorTest {
  private Genson genson = new Genson();

  @Test
  public void testFailFastBeanDescriptorWithWrongType() {
    BeanDescriptorProvider provider = new GensonBuilder() {
      protected BeanDescriptorProvider createBeanDescriptorProvider() {
        return new BaseBeanDescriptorProvider(new ContextualConverterFactory(null),
          new BeanPropertyFactory.CompositeFactory(Arrays
            .asList(new BeanPropertyFactory.StandardFactory())),
          getMutatorAccessorResolver(), getPropertyNameResolver(),
          true, true, true) {
          @Override
          @SuppressWarnings({"unchecked", "rawtypes"})
          protected <T> com.owlike.genson.reflect.BeanDescriptor<T> create(
            java.lang.Class<T> forClass,
            java.lang.reflect.Type ofType,
            com.owlike.genson.reflect.BeanCreator creator,
            java.util.List<com.owlike.genson.reflect.PropertyAccessor> accessors,
            java.util.Map<String, com.owlike.genson.reflect.PropertyMutator> mutators,
            Genson genson) {
            return new BeanDescriptor(ThatObject.class, ThatObject.class, accessors,
              mutators, creator, false);
          }
        };
      }
    }.create().getBeanDescriptorProvider();
    try {
      provider.provide(AnotherObject.class, ThatObject.class, genson);
      fail();
    } catch (ClassCastException cce) {
      // OK
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConverterChain() {
    Genson genson = new GensonBuilder() {
      @Override
      protected Factory<Converter<?>> createConverterFactory() {
        return new BasicConvertersFactory(
          getSerializersMap(), getDeserializersMap(), getFactories(),
          getBeanDescriptorProvider());
      }
    }.useConstructorWithArguments(true).create();

    @SuppressWarnings("rawtypes")
    BeanDescriptor<ThatObject> pDesc = (BeanDescriptor) genson
      .provideConverter(ThatObject.class);
    assertEquals(DefaultConverters.StringConverter.class,
      pDesc.mutableProperties.get("aString").propertyDeserializer.getClass());
    assertEquals(DefaultConverters.IntegerConverter.class,
      pDesc.mutableProperties.get("aPrimitive").propertyDeserializer.getClass());
    assertEquals(DefaultConverters.CollectionConverter.class,
      pDesc.mutableProperties.get("listOfDates").propertyDeserializer.getClass());
    @SuppressWarnings("rawtypes")
    CollectionConverter<Object> listOfDateConverter = (CollectionConverter) pDesc.mutableProperties
      .get("listOfDates").propertyDeserializer;
    assertEquals(DefaultConverters.DateConverter.class, listOfDateConverter
      .getElementConverter().getClass());

    assertEquals(BeanDescriptor.class,
      pDesc.mutableProperties.get("anotherObject").propertyDeserializer.getClass());
  }

  @Test
  public void genericTypeTest() {
    BaseBeanDescriptorProvider provider = createBaseBeanDescriptorProvider();

    BeanDescriptor<SpecilizedClass> bd = provider.provide(SpecilizedClass.class,
      SpecilizedClass.class, new Genson());
    assertEquals(B.class, getAccessor("t", bd).type);
    assertEquals(B.class,
      ((GenericArrayType) getAccessor("tArray", bd).type).getGenericComponentType());
    assertEquals(Double.class, getAccessor("value", bd).type);
  }

  @Test
  public void jsonWithJsonIgnore() throws SecurityException, NoSuchFieldException {
    BeanMutatorAccessorResolver strategy = new BeanMutatorAccessorResolver.CompositeResolver(
      Arrays.asList(new BeanMutatorAccessorResolver.GensonAnnotationsResolver(),
        new BeanMutatorAccessorResolver.StandardMutaAccessorResolver()));

    assertFalse(strategy.isAccessor(
      ClassWithIgnoredProperties.class.getDeclaredField("ignore"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertFalse(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("ignore"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertTrue(strategy.isAccessor(ClassWithIgnoredProperties.class.getDeclaredField("a"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertFalse(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("a"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertTrue(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("b"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertFalse(strategy.isAccessor(ClassWithIgnoredProperties.class.getDeclaredField("b"),
      ClassWithIgnoredProperties.class).booleanValue());
  }

  @Test
  public void jsonInclusionWithJsonProperty() throws SecurityException, NoSuchFieldException {
    BeanMutatorAccessorResolver strategy = new BeanMutatorAccessorResolver.CompositeResolver(
      Arrays.asList(new BeanMutatorAccessorResolver.GensonAnnotationsResolver(),
        new BeanMutatorAccessorResolver.StandardMutaAccessorResolver()));

    assertTrue(strategy.isAccessor(ClassWithIgnoredProperties.class.getDeclaredField("p"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertTrue(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("p"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertFalse(strategy.isAccessor(ClassWithIgnoredProperties.class.getDeclaredField("q"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertTrue(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("q"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertFalse(strategy.isMutator(ClassWithIgnoredProperties.class.getDeclaredField("r"),
      ClassWithIgnoredProperties.class).booleanValue());
    assertTrue(strategy.isAccessor(ClassWithIgnoredProperties.class.getDeclaredField("r"),
      ClassWithIgnoredProperties.class).booleanValue());
  }

  PropertyAccessor getAccessor(String name, BeanDescriptor<?> bd) {
    for (PropertyAccessor a : bd.accessibleProperties)
      if (name.equals(a.name)) return a;
    return null;
  }

  @Test
  public void testOneCreatorPerClass() {
    try {
      genson.provideConverter(MultipleCreator.class);
      fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testUseExplicitMethodCtr() {
    genson.deserialize("{}", ForceMethodCreator.class);
    assertTrue(ForceMethodCreator.usedMethod);
  }

  @Test
  public void testUseExplicitConstructorCtr() {
    genson.deserialize("{}", ForceConstructorCreator.class);
    assertTrue(ForceConstructorCreator.usedCtr);
  }

  @Test public void testCandidateFieldAnnotationsShouldBeMergedWithSelectedMethodAnnotations() {
    BaseBeanDescriptorProvider provider = createBaseBeanDescriptorProvider();

    BeanDescriptor<SomeClass> bd = provider.provide(SomeClass.class, genson);

    assertNotNull(getAccessor("name", bd).getAnnotation(MyAnn.class));
    assertNotNull(getAccessor("name", bd).getAnnotation(AnotherAnn.class));

    assertNotNull(bd.mutableProperties.get("name").getAnnotation(MyAnn.class));
    assertNotNull(bd.mutableProperties.get("name").getAnnotation(AnotherAnn.class));

    assertNotNull(bd.creator.getProperties().get("name").getAnnotation(MyAnn.class));
    assertNotNull(bd.creator.getProperties().get("name").getAnnotation(AnotherAnn.class));
  }

  private BaseBeanDescriptorProvider createBaseBeanDescriptorProvider() {
    return new BaseBeanDescriptorProvider(
      new ContextualConverterFactory(null), new BeanPropertyFactory.CompositeFactory(
      Arrays.asList(new BeanPropertyFactory.StandardFactory())),
      new BeanMutatorAccessorResolver.StandardMutaAccessorResolver(),
      new PropertyNameResolver.CompositePropertyNameResolver(Arrays.asList(
        new ASMCreatorParameterNameResolver(true),
        new PropertyNameResolver.ConventionalBeanPropertyNameResolver()
      )), true, true, true);
  }

  @Test public void propertyWithSameNameShouldOverrideParent() {
    Child child = new Child();
    child.a = 1;
    child.setB(2);
    child.setD(3);
    child.c = 4;

    String json = genson.serialize(child);
    assertEquals("{\"a\":1,\"b\":2,\"c\":4,\"d\":3}", json);
    Child actual = genson.deserialize(json, Child.class);

    assertEquals(1, actual.a);
    assertEquals(2, actual.getB());
    assertEquals(3, actual.getD());
    assertEquals(4, actual.c);
    assertNull(((Parent)actual).a);
    assertNull(actual.b);
    assertNull(actual.getC());
  }

  public static class Parent {
    public String a;
    public String b;
    private String c;

    public void setC(String c) {
      this.c = c;
    }
    public String getC() {
      return c;
    }
  }

  public static class Child extends Parent {
    public int a;
    public int c;
    public String d;
    private int _b;

    public void setD(int d) {
      this.d = ""+d;
    }
    public int getD() {
      return Integer.valueOf(d);
    }
    public void setB(int b) {
      this._b = b;
    }
    public int getB() {
      return _b;
    }
  }

  static class ForceMethodCreator {
    public static transient boolean usedMethod = false;

    ForceMethodCreator() {
    }

    @JsonCreator
    public static ForceMethodCreator create() {
      usedMethod = true;
      return new ForceMethodCreator();
    }
  }

  static class ForceConstructorCreator {
    public static transient boolean usedCtr = false;

    ForceConstructorCreator() {
    }

    @JsonCreator
    ForceConstructorCreator(@JsonProperty("i") Integer iii) {
      usedCtr = true;
    }
  }

  static class MultipleCreator {
    @JsonCreator
    MultipleCreator() {
    }

    @JsonCreator
    public static MultipleCreator create() {
      return null;
    }
  }

  public static class B {
    public String v;
  }

  public static class ClassWithGenerics<T, E extends Number> {
    public T t;
    public T[] tArray;
    public E value;

    public void setT(T t) {
      this.t = t;
    }
  }

  public static class SpecilizedClass extends ClassWithGenerics<B, Double> {

  }

  public class ClassWithIgnoredProperties {
    @JsonIgnore
    public String ignore;
    @JsonIgnore(serialize = true)
    String a;
    @JsonIgnore(deserialize = true)
    public String b;

    @JsonProperty
    transient int p;
    @JsonProperty(serialize = false)
    private transient int q;
    @JsonProperty(deserialize = false)
    public transient int r;
  }


  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.FIELD, ElementType.METHOD})
  public @interface MyAnn {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.FIELD, ElementType.METHOD})
  public @interface AnotherAnn {}

  public static class SomeClass {
    @MyAnn
    public String name;
    public Integer age;

    public SomeClass(String name, Integer age) {}

    @AnotherAnn
    public String getName() {
      return name;
    }

    @AnotherAnn
    public void setName(String name) {
      this.name = name;
    }

    @MyAnn
    @AnotherAnn
    public Integer getAge() {
      return age;
    }

    @MyAnn
    public void setAge(Integer age) {
      this.age = age;
    }
  }


  private static class ThatObject {
    @SuppressWarnings("unused")
    String aString;
    @SuppressWarnings("unused")
    int aPrimitive;
    @SuppressWarnings("unused")
    List<Date> listOfDates;

    @SuppressWarnings("unused")
    public ThatObject(AnotherObject anotherObject) {
    }
  }

  private static class AnotherObject {
    @SuppressWarnings("unused")
    public AnotherObject() {
    }
  }
}
