package com.ar.core.data

/**
 * Fetch strategy for repository read operations.
 *
 * - FAST: Prefer local cache (device-first), then (if online) also listen/fetch remote updates.
 * - FRESH: Prefer remote (cloud-first). If offline, returns error (no fallback).
 * - CACHED: Offline-only; reads only from local database.
 * - FALLBACK: Try remote first; if it fails/offline, fall back to local.
 * - SYNCED: Fetch remote once, update local cache, then return the remote result.
 */
enum class FetchStrategy {
    FAST,
    FRESH,
    CACHED,
    FALLBACK,
    SYNCED
}
