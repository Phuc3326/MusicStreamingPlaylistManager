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
            if (song.getTitle().toLowerCase().contains(kw)) {
                results.add(song);
            }
        }

        for (int i = 0; i < start; i++) {
            Song song = sortedLibrary.get(i);
            if (song.getTitle().toLowerCase().contains(kw)) {
                results.add(song);
            }
        }

        return results;
    }

    public DynamicArrayList filterByGenre(String genre) {
        DynamicArrayList results = new DynamicArrayList();
        if (genre == null || genre.trim().isEmpty()) {
            return results;
        }

        String target = genre.trim().toLowerCase();
        for (int i = 0; i < sortedLibrary.size(); i++) {
            Song song = sortedLibrary.get(i);
            if (song.getGenre().equalsIgnoreCase(target)) {
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
