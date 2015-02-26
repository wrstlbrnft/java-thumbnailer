/*
 * regain/Thumbnailer - A file search engine providing plenty of formats (Plugin)
 * Copyright (C) 2011  Come_IN Computerclubs (University of Siegen)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Contact: Come_IN-Team <come_in-team@listserv.uni-siegen.de>
 */
package de.uni_siegen.wineme.come_in.thumbnailer.util;



import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Hashtable that can contain several entries per key.
 * (Helper Class)
 *
 * Contract:
 * <li>It is possible to put several identical key-value pairs (i.e. where key and value is equal)
 * <li>entrySet is not supported. Instead, it can be iterated over all entries.
 *
 * @param <K>	Key
 * @param <V>	Value
 */
public class ChainedHashMap<K, V> implements Map<K, V>, Iterable<Map.Entry<K, V>> {

	private static final int DEFAULT_HASHTABLE_SIZE = 20;

	private static final int DEFAULT_LIST_SIZE = 10;
	private int listSize;

	HashMap<K, List<V>> hashtable;
	int size;

	public ChainedHashMap()
	{
		this(ChainedHashMap.DEFAULT_HASHTABLE_SIZE);
	}

	public ChainedHashMap(final int hashtableSize) {
		this(hashtableSize, ChainedHashMap.DEFAULT_LIST_SIZE);
	}
	public ChainedHashMap(final int hashtableSize, final int chainSize) {
		this.hashtable = new HashMap<K, List<V>>(hashtableSize);
		this.listSize = chainSize;

		this.size = 0;
	}

	public ChainedHashMap(final Map<? extends K, ? extends V> map)
	{
		this();

		if (map instanceof ChainedHashMap)
		{
			// Copy-constructor
			final ChainedHashMap<? extends K, ? extends V> hashtable = (ChainedHashMap<? extends K, ? extends V>) map;
			for (final K key : hashtable.keySet())
			{
				for (final V value: hashtable.getList(key))
				{
					this.put(key, value);
				}
			}
		} else {
      this.putAll(map);
    }
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		return this.hashtable.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		if (this.isEmpty()) {
      return false;
    }

		final Collection<List<V>> elements = this.hashtable.values();

		for (final List<V> list: elements)
		{
			if (list.contains(value)) {
        return true;
      }
		}
		return false;
	}

	@Override
	/**
	 * Get first of the linked objects by this key.
	 */
	public V get(final Object key) {
		final List<V> list = this.hashtable.get(key);
		if (list == null) {
      return null;
    } else {
      return list.get(0);
    }
	}

	/**
	 * Get all objects linked by this key
	 * as an Iterable usable an foreach loop.
	 *
	 * @param key
	 * @return	Iterable
	 * @throws NullPointerException (if key null)
	 */
	public Iterable<V> getIterable(final Object key) {
		final List<V> list = this.hashtable.get(key);

		if (list == null)
		{
			// Empty Iterator
			return new Iterable<V>() {
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						public boolean hasNext() { return false; }
						public V next() { throw new NoSuchElementException("Empty"); }
						public void remove() { }
					};
				}
			};
		}
		else
		{
			// Iterator of list
			return new Iterable<V>() {
				public Iterator<V> iterator() {
					return list.iterator();
				}
			};
		}
	}

	public List<V> getList(final Object key)
	{
	  List<V> list = this.hashtable.get(key);
		if (list == null) {
      list = new ArrayList<V>();
    }
		return list;
	}

	/**
	 * Iterate over all elements in the table.
	 * Note that this currently copies them into a collection,
	 * so concurrent modification will not be taken into account
	 * (there will be no ConcurrentModificationException, either).
	 */
	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		if (this.size == 0)
		{
			return new Iterator<Map.Entry<K, V>>() {
				public boolean hasNext() { return false; }
				public Map.Entry<K, V> next() { throw new NoSuchElementException("Empty"); }
				public void remove() { }
			};
		}
		else
		{
			final Collection<Map.Entry<K, V>> entries = new ArrayList<Map.Entry<K, V>>();
			for (final K key : this.hashtable.keySet())
			{
				final List<V> values = this.hashtable.get(key);
				for (final V value : values) {
          entries.add(new AbstractMap.SimpleEntry<K,V>(key, value));
        }
			}
			return entries.iterator();
		}
	}

	@Override
	/**
	 * Add this Value at the end of this key.
	 *
	 * @return As the value is never replaced, this will always return null.
	 */
	public V put(final K key, final V value) {
		boolean success;

		List<V> list = this.hashtable.get(key);
		if (list == null)
		{
			list = new ArrayList<V>(this.listSize);
			success = list.add(value);
			this.hashtable.put(key, list);
		}
		else
		{
			success = list.add(value);
		}

		if (success) {
      this.size++;
    }

		return null;
	}

	@Override
	/**
	 * Remove all objects linked to this key.
	 *
	 * @param key	Key
	 * @return First of linked objects (or null).
	 */
	public V remove(final Object key) {
		final List<V> list = this.hashtable.remove(key);
		if (list == null) {
      return null;
    } else
		{
			final V element = list.get(0);
			this.size -= list.size();
			return element;
		}
	}


	public boolean removeByKeyAndValue(final K key, final V value)
	{
		final List<V> list = this.hashtable.get(key);

		if (list == null) {
      return false;
    }

		final boolean removed = list.remove(value);
		if (removed)
		{
			if (list.isEmpty()) {
        this.hashtable.remove(key);
      }
			this.size--;
		}
		return removed;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		this.hashtable.clear();
		this.size = 0;
	}

	@Override
	public Set<K> keySet() {
		return this.hashtable.keySet();
	}

	@Override
	// TODO "The set is backed by the map, so changes to the map are reflected in the set, and vice-versa."
	public Collection<V> values() {
		final List<V> newList = new ArrayList<V>();

		if (this.isEmpty()) {
      return newList;
    }

		final Collection<List<V>> values = this.hashtable.values();

		for(final List<V> list : values ) {
      newList.addAll(list);
    }

		return newList;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException("entrySet is not implemented, as identical entries are allowed (conflict with Set contract). Instead, use .iterator() to iterate through all entries.");
	}

	@Override
  public String toString()
	{
		final StringBuffer str = new StringBuffer(200);

		for (final K key : this.hashtable.keySet())
		{
			str.append(key).append(":\n");

			final List<V> values = this.hashtable.get(key);
			for (final V value : values) {
        str.append("\t").append(value).append("\n");
      }
			str.append("\n");
		}

		return str.toString();
	}


}
