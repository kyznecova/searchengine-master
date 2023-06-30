package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;



    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
            if (siteEntity != null && siteEntity.getStatus().equals(StatusType.INDEXED)) {
                int pages = 0;
                int lemmas = 0;
                List<PageEntity> pageEntityList = pageRepository.findAll();
                for (PageEntity page : pageEntityList) {
                    if (page.getSiteId().getId() == siteEntity.getId()) {
                        pages++;
                    }
                }
                List<LemmaEntity> lemmaEntityList = lemmaRepository.findAll();
                for (LemmaEntity lemma : lemmaEntityList) {
                    if (lemma.getSiteId().getId() == siteEntity.getId()) {
                        lemmas++;
                    }
                }
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(String.valueOf(siteEntity.getStatus()));
                item.setError(siteEntity.getLastError());
                long date = (ZonedDateTime.of(siteEntity.getStatusTime(), ZoneId.systemDefault()))
                        .toInstant().toEpochMilli();
                item.setStatusTime(date);
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
            }
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

}
