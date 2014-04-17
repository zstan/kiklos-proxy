package target.eyes.vag.codec.xml.javolution.vast.impl;

import javolution.xml.XMLSerializable;

public class VASTXMLWrapper<T extends XMLSerializable> {

	private Class<T> clazz;

	private String name;

	private T object;

	protected VASTXMLWrapper(T object, String name, Class<T> cls) {
		this.clazz = cls;
		this.name = name;
		this.object = object;
	}

	public static <T extends XMLSerializable> VASTXMLWrapper<T> createInstance(
			T object, String name, Class<T> cls) {
		return new VASTXMLWrapper<T>(object, name, cls);
	}

	public Class<T> getClazz() {
		return clazz;
	}

	public String getName() {
		return name;
	}

	public T getObject() {
		return object;
	}
}
