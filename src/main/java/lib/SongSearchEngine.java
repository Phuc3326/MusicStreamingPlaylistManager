package lib;

import models.Song;

public class SongSearchEngine {
    private final DynamicArrayList sortedLibrary;

    public SongSearchEngine(DynamicArrayList library) {
        this.sortedLibrary = MergeSort.sortByTitle(library);
    }

    public DynamicArrayList getSortedLibrary() {
        return sortedLibrary;
    }

    public DynamicArrayList searchByTitle(String keyword) {
        DynamicArrayList results = new DynamicArrayList();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String kw = keyword.trim().toLowerCase();
        int start = lowerBoundPrefix(kw);

        for (int i = start; i < sortedLibrary.size(); i++) {
            Song song = sortedLibrary.get(i);
            String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
            String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
            String genre = song.getGenre() != null ? song.getGenre().toLowerCase() : "";
            if (title.contains(kw) || artist.contains(kw) || genre.contains(kw)) {
                results.add(song);
            }
        }

        for (int i = 0; i < start; i++) {
            Song song = sortedLibrary.get(i);
            String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
            String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
            String genre = song.getGenre() != null ? song.getGenre().toLowerCase() : "";
            if (title.contains(kw) || artist.contains(kw) || genre.contains(kw)) {
                results.add(song);
            }
        }

        return results;
    }

    public Song binarySearchExactTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String target = title.trim();
        int lo = 0;
        int hi = sortedLibrary.size() - 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            Song song = sortedLibrary.get(mid);
            int cmp = song.getTitle().compareToIgnoreCase(target);

            if (cmp == 0) {
                return song;
            }
            if (cmp < 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return null;
    }

    public DynamicArrayList filterByGenre(String genre) {
        DynamicArrayList results = new DynamicArrayList();
        if (genre == null || genre.trim().isEmpty()) {
            return results;
        }

        String target = genre.trim().toLowerCase();
        for (int i = 0; i < sortedLibrary.size(); i++) {
            Song song = sortedLibrary.get(i);
            String sg = song.getGenre() != null ? song.getGenre().trim().toLowerCase() : "";
            if (sg.equals(target) || sg.contains(target) || target.contains(sg)) {
                results.add(song);
            }
        }
        return results;
    }

    private int lowerBoundPrefix(String keyword) {
        int lo = 0;
        int hi = sortedLibrary.size();

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            String title = sortedLibrary.get(mid).getTitle().toLowerCase();
            if (title.compareTo(keyword) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        return lo;
    }
}
