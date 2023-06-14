package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    private boolean indexingStarted;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private SiteEntity siteEntity;
    private ForkJoinPool forkJoinPool;

    @Override
    public IndexingResponse startIndexing() {
        if (indexingStarted){
            return new IndexingResponse(false, "Индексация уже запущена");
        } else {
            indexingStarted = true;
        }

        pageRepository.deleteAll();
        siteRepository.deleteAll();

        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(StatusType.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

            forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            SiteMapCreator siteMapCreator = new SiteMapCreator(
                    site.getUrl(),
                    siteEntity,
                    pageRepository,
                    siteRepository);
            forkJoinPool.execute(siteMapCreator);
            forkJoinPool.shutdown();

            siteEntity.setStatus(StatusType.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
        indexingStarted = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!indexingStarted) {
            return new IndexingResponse(false, "Индексация не запущена");
        } else {
            indexingStarted = false;
        }
        forkJoinPool.shutdownNow();
        List<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == StatusType.INDEXING) {
                site.setStatus(StatusType.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexPage(String url) {
        return null;
    }
}
