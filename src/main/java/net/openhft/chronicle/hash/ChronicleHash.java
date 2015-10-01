/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.hash;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.MapMethods;
import net.openhft.chronicle.map.MapQueryContext;
import net.openhft.chronicle.set.ChronicleSet;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This interface defines common {@link ChronicleMap} and {@link ChronicleSet}, related to off-heap
 * memory management and file-mapping. Not usable by itself.
 */
public interface ChronicleHash<K, E extends HashEntry<K>, SC extends HashSegmentContext<K, ?>,
        EQC extends ExternalHashQueryContext<K>> extends Closeable {
    /**
     * Returns the file this hash container mapped to, i. e. when it is created by
     * {@link ChronicleHashBuilder#create()} call, or {@code null} if it is purely in-memory,
     * i. e. if it is created by {@link ChronicleHashBuilder#create()} call.
     *
     * @return the file this {@link ChronicleMap} or {@link ChronicleSet} is mapped to,
     *         or {@code null} if it is not mapped to any file
     * @see ChronicleHashBuilder#createPersistedTo(File)
     */
    File file();

    long longSize();

    /**
     * @return the class of {@code <K>}
     */
    Class<K> keyClass();

    /**
     * Returns the context to perform arbitrary operations with the given key in this map.
     * Conventionally, try-with-resources block should wrap the returned context: <pre>{@code
     * try (ExternalHashQueryContext<K> q = hash.queryContext(key)) {
     *     // ... do something
     * }}</pre>
     * See {@link HashQueryContext} and {@link MapMethods} for a lot of inspiration about using this
     * functionality.
     *
     * @param key the queried key
     * @return the context to perform operations with the key
     * @see HashQueryContext
     * @see MapQueryContext
     * @see ExternalHashQueryContext
     * @see MapMethods
     */
    @NotNull
    EQC queryContext(K key);

    /**
     * Equivalent to {@link #queryContext(Object)}, but accepts {@code Data} instead of key as
     * an object. Useful, when you already have {@code Data}, calling this method instead of {@link
     * #queryContext(Object)} might help to avoid unnecessary deserialization.
     *
     * @param key the queried key as {@code Data}
     * @return the context to perform operations with the key
     */
    @NotNull EQC queryContext(Data<K> key);

    /**
     * Returns the context of the segment with the given index. Segments are indexed from 0 to
     * {@link #segments()}{@code - 1}.
     *
     * @see HashSegmentContext
     */
    SC segmentContext(int segmentIndex);

    /**
     * Returns the number of segments in this {@code ChronicleHash}.
     *
     * @see ChronicleHashBuilder#minSegments(int)
     * @see ChronicleHashBuilder#actualSegments(int)
     */
    int segments();

    /**
     * Checks the given predicate on each entry in this {@code ChronicleHash} until all entries
     * have been processed or the predicate returns {@code false} for some entry, or throws
     * an {@code Exception}. Exceptions thrown by the predicate are relayed to the caller.
     *
     * <p>The order in which the entries will be processed is unspecified. It might differ from
     * the order of iteration via {@code Iterator} returned by any method of this
     * {@code ChronicleHash} or it's collection view.
     *
     * <p>If the {@code ChronicleHash} is empty, this method returns {@code true} immediately.
     *
     * @param predicate the predicate to be checked for each entry
     * @return {@code true} if the predicate returned {@code true} for all entries of
     * the {@code ChronicleHash}, {@code false} if it returned {@code false} for the entry
     */
    boolean forEachEntryWhile(Predicate<? super E> predicate);

    /**
     * Performs the given action for each entry in this {@code ChronicleHash} until all entries have
     * been processed or the action throws an {@code Exception}. Exceptions thrown by the action are
     * relayed to the caller.
     *
     * <p>The order in which the entries will be processed is unspecified. It might differ from
     * the order of iteration via {@code Iterator} returned by any method of this
     * {@code ChronicleHash} or it's collection view.
     *
     * @param action the action to be performed for each entry
     */
    void forEachEntry(Consumer<? super E> action);

    /**
     * Releases the off-heap memory, used by this hash container and resources, used by replication,
     * if any. However, if hash container (hence off-heap memory, used by it) is mapped to the file
     * and there are other instances mapping the same data on the server across JVMs, the memory
     * won't be actually freed on operating system level. I. e. this method call doesn't affect
     * other {@link ChronicleMap} or {@link ChronicleSet} instances mapping the same data.
     *
     * <p>If you won't call this method, memory would be held at least until next garbage
     * collection. This could be a problem if, for example, you target rare garbage collections,
     * but load and drop {@code ChronicleHash}es regularly.
     *
     * <p>
     * TODO what about commit guarantees, when ChronicleMap is used with memory-mapped files, if
     * {@code ChronicleMap}/{@code ChronicleSet} closed/not?
     *
     * <p>After this method call behaviour of <i>all</i> methods of {@code ChronicleMap}
     * or {@code ChronicleSet} is undefined. <i>Any</i> method call on the map might throw
     * <i>any</i> exception, or even JVM crash. Shortly speaking, don't call use map closing.
     */
    @Override
    void close();

    /**
     * Tells whether or not this {@code ChronicleHash} (on-heap instance) is open.
     *
     * @return {@code true} is {@link #close()} is not yet called
     */
    boolean isOpen();
}
