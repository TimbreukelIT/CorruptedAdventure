/**
 * Common mixins only (config {@code corruptedadventure.common.mixins.json}, section {@code "mixins"}): they are loaded
 * on the dedicated server too, so they may never name a client class ({@code ClientIsolationTest} checks this package).
 * Client-only mixins go in {@code client/mixin} with the client config {@code corruptedadventure.mixins.json}. Mixin
 * forbids loading any other class from this package, so helpers and tests live elsewhere.
 */
package nl.timtoys.corruptedadventure.mixin;
