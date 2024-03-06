// Given an array of strings, return the count of the number of strings with the given length.


// wordsCount(["a", "bb", "b", "ccc"], 1) → 2
// wordsCount(["a", "bb", "b", "ccc"], 3) → 1
// wordsCount(["a", "bb", "b", "ccc"], 4) → 0

public int wordsCount(String[] words, int len) {
    int count = 0;
    for(String i: words) {
      if(i.length() == len) count++;
    }
    return count;
  }
  