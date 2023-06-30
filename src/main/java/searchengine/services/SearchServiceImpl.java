package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private final LemmaFinder lemmaFinder;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final SnippetGenerator snippetGenerator;
    private final int excludedPercent = 2;
    @Override
    public SearchResponse getSearchResults(String query, String siteUrl, Integer offset, Integer limit) {

        if(query.isEmpty()) {
            return new SearchResponse(false, "Задан пустой поисковый запрос", 0, new ArrayList<>());
        }

        Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);
        LinkedHashMap<String, Integer> sortedLemmas = sortLemmas(queryLemmas);
        SearchResponse searchResponse = new SearchResponse();
        LinkedHashMap<LemmaEntity, PageEntity> entitiesList = new LinkedHashMap<>();

        if (siteUrl != null) {
            SiteEntity siteEntity = siteRepository.findSiteEntityByUrlIsIgnoreCase(siteUrl);
            entitiesList = getEntitiesList(queryLemmas, siteEntity, sortedLemmas);
        } else {
            for (Site site : sitesList.getSites()) {
                SiteEntity siteEntity = siteRepository.findSiteEntityByUrlIsIgnoreCase(site.getUrl());
                entitiesList.putAll(getEntitiesList(queryLemmas, siteEntity, sortedLemmas));
            }
        }

        LinkedHashMap<PageEntity, Integer> pagesRelevance = countAbsoluteRank(entitiesList);
        LinkedHashMap<PageEntity, Integer> sortedPages = sortPages(pagesRelevance);
        List<SearchData> generatedSearchDataList = generateSearchDataList(sortedPages, queryLemmas, limit, offset);
        searchResponse = response(generatedSearchDataList);
        return searchResponse;
    }
    private LinkedHashMap<String, Integer> sortLemmas(Set<String> lemmasList) {

        LinkedHashMap<String, Integer> foundLemmas = new LinkedHashMap<>();
        for (String lemma : lemmasList) {
            AtomicInteger frequency = new AtomicInteger();
            List<LemmaEntity> lemmas = lemmaRepository.findLemmaEntitiesByLemmaEqualsIgnoreCase(lemma);
            lemmas = removeMostFrequentlyLemmas(lemmas);
            lemmas.forEach(lemmaEntity -> frequency.set(frequency.get() + lemmaEntity.getFrequency()));
            foundLemmas.put(lemma, frequency.intValue());
        }
        LinkedHashMap<String, Integer> sortedLemmas = foundLemmas.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new));
        return sortedLemmas;
    }

    private ArrayList<LemmaEntity> removeMostFrequentlyLemmas(List<LemmaEntity> lemmas) {
        ArrayList<LemmaEntity> result = new ArrayList<>(lemmas);
        int removeCount = Math.round((float) lemmas.size() / 100 * excludedPercent);

        LemmaEntity removable = new LemmaEntity();
        for (int i = 0; i < removeCount; i++) {
            int maxFrequency = 0;
            for (LemmaEntity lemma : lemmas) {
                if (lemma.getFrequency() > maxFrequency) {
                    maxFrequency = lemma.getFrequency();
                    removable = lemma;
                }
            }
            result.remove(removable);
        }
        return result;
    }
    private LinkedHashMap<LemmaEntity, PageEntity> getEntitiesList(Set<String> queryLemmas,
                                                                   SiteEntity site,
                                                                   LinkedHashMap<String, Integer> sortedLemmas) {
        List<PageEntity> pagesListFromFirstLemma = new ArrayList<>();
        ArrayList<String> lemmaList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            lemmaList.add(entry.getKey());
        }
        String minFrequencyLemma = lemmaList.get(0);

        ArrayList<IndexEntity> indexesFromFirstLemma =
                indexRepository.findIndexEntityByLemmaId_LemmaAndPageId_SiteId(minFrequencyLemma, site);
        indexesFromFirstLemma.forEach(index -> pagesListFromFirstLemma.add(index.getPageId()));

        List<PageEntity> refactoredList = new ArrayList<>(pagesListFromFirstLemma);
        ArrayList<String> lemmasList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            lemmaList.add(entry.getKey());
        }
        if (lemmasList.size() > 0) {
            lemmasList.remove(0);

            for (PageEntity page : pagesListFromFirstLemma) {
                for (String lemma : lemmasList) {
                    if (indexRepository.findIndexEntityByPageIdAndLemmaId_Lemma(page, lemma).isEmpty()) {
                        refactoredList.remove(page);
                    }
                }
            }
        }

        LinkedHashMap<LemmaEntity, PageEntity> finalPagesAndLemmasList = new LinkedHashMap<>();
        for (PageEntity page : refactoredList) {
            for (String lemma : queryLemmas) {
                indexRepository.findIndexEntityByPageIdAndLemmaId_Lemma(page, lemma)
                        .forEach(index ->
                                finalPagesAndLemmasList.put(index.getLemmaId(), index.getPageId()));
            }
        }
        return finalPagesAndLemmasList;
    }

    private LinkedHashMap<PageEntity, Integer> countAbsoluteRank(LinkedHashMap<LemmaEntity, PageEntity> lemmaAndPageList) {

        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        for (Map.Entry<LemmaEntity, PageEntity> entry : lemmaAndPageList.entrySet()) {
            if (sortedList.containsKey(entry.getValue())) {
                int rank = sortedList.get(entry.getValue());
                sortedList.remove(entry.getValue());
                sortedList.put(entry.getValue(), (entry.getKey().getFrequency() + rank));
            } else {
                sortedList.put(entry.getValue(), entry.getKey().getFrequency());
            }
        }
        return sortedList;
    }

    private LinkedHashMap<PageEntity, Integer> sortPages(LinkedHashMap<PageEntity, Integer> finalPages) {
        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        sortedList = finalPages.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new));
        return sortedList;
    }

    private SearchData generateSearchData(String site,
                                          String siteName,
                                          String uri,
                                          String title,
                                          String snippet,
                                          float relevance) {
        return SearchData.builder()
                .site(site)
                .siteName(siteName)
                .uri(uri)
                .title(title)
                .snippet(snippet)
                .relevance(relevance)
                .build();
    }
    private List<SearchData> generateSearchDataList(LinkedHashMap<PageEntity,
            Integer> sortedPages, Set<String> lemmasFromQuery, int limit, int offset) {
        if (offset != 0 && sortedPages.size() > 0) {
            sortedPages.remove(sortedPages.keySet().stream().findFirst().get());
        }
        List<SearchData> dataList = new ArrayList<>();
        int count = 0;
        for (Map.Entry<PageEntity, Integer> entry : sortedPages.entrySet()) {
            if (count < limit) {
                dataList.add(
                        generateSearchData(
                                entry.getKey().getSiteId().getUrl(),
                                entry.getKey().getSiteId().getName(),
                                entry.getKey().getPath(),
                                Jsoup.parse(entry.getKey().getContent()).title(),
                                getSnippet(entry.getKey(), lemmasFromQuery),
                                entry.getValue()));
                count++;
            }
        }
        return dataList;
    }

    private String getSnippet(PageEntity page, Set<String> lemmas) {
        List<String> queryList = new ArrayList<>(lemmas);
        snippetGenerator.setText(page.getContent());
        snippetGenerator.setQueryWords(queryList);
        return snippetGenerator.generateSnippets();
    }

    private SearchResponse response(List<SearchData> searchData) {
        return SearchResponse.builder()
                .result(true)
                .count(searchData.size())
                .data(searchData)
                .build();
    }
}
