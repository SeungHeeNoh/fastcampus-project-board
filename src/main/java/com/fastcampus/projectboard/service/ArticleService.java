package com.fastcampus.projectboard.service;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.type.SearchType;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ArticleService {

    private final ArticleRepository articleRepository;


    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(SearchType searchType, String searchKeyword, Pageable pageable) {
        if(searchKeyword == null || searchKeyword.isBlank()) {
            return articleRepository.findAll(pageable).map(ArticleDto::from);
        }

        return switch (searchType) {
            case TITLE -> articleRepository.findByTitleContaining(searchKeyword, pageable).map(ArticleDto::from);
            case CONTENT -> articleRepository.findByContentContaining(searchKeyword, pageable).map(ArticleDto::from);
            case ID -> articleRepository.findByUserAccount_UserIdContaining(searchKeyword, pageable).map(ArticleDto::from);
            case NICKNAME -> articleRepository.findByUserAccount_NicknameContaining(searchKeyword, pageable).map(ArticleDto::from);
            case HASHTAG -> articleRepository.findByHashtag("#" + searchKeyword, pageable).map(ArticleDto::from);
        };
    }

    @Transactional(readOnly = true)
    public ArticleDto searchArticle(long articleId) {
        return null;
    }

    public void saveArticle(ArticleDto dto) {
        articleRepository.save(dto.toEntity());
    }

    public void updateArticle(ArticleDto dto) {
        try {
            // getReferenceById
            Article article = articleRepository.getReferenceById(dto.id());
            if (dto.title() != null) {
                article.setTitle(dto.title());
            }

            if (dto.content() != null) {
                article.setContent(dto.content());
            }

            article.setHashtag(dto.hashtag());
            //        articleRepository.save(article);
            //        => @Transactional 애노테이션에 의해 해당 메소드가 종료될 때 article 변경된 것을 감지하고 update 쿼리를 날린다.
        } catch (EntityNotFoundException e) {
            log.warn("게시글 업데이트 실패. 게시글을 찾을 수 없습니다. - dto: {}", dto);
        }
    }

    public void deleteArticle(long articleId) {
        articleRepository.deleteById(articleId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getArticle(Long articleId) {
        Map<String, Object> resultMap = new HashMap<>();

        ArticleWithCommentsDto articleWithCommentsDto = articleRepository.findById(articleId).map(ArticleWithCommentsDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다. - articleId : " + articleId));

        resultMap.put("article", articleWithCommentsDto);
        List<Long> prevNextArticleIds = articleRepository.findPrevNextArticleIds(articleWithCommentsDto.id());

        Long prevId = prevNextArticleIds.size() > 1 ? prevNextArticleIds.get(0) : (!prevNextArticleIds.isEmpty() && prevNextArticleIds.get(0) < articleId ? prevNextArticleIds.get(0) : null);
        Long nextId = prevNextArticleIds.size() > 1 ? prevNextArticleIds.get(1) : (!prevNextArticleIds.isEmpty() && prevNextArticleIds.get(0) > articleId ? prevNextArticleIds.get(0) : null);

        resultMap.put("prevId", prevId);
        resultMap.put("nextId", nextId);

        return resultMap;
    }

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticlesViaHashtag(String hashtag, Pageable pageable) {
        if(hashtag == null || hashtag.isBlank()) {
            return Page.empty(pageable);
        }

        return articleRepository.findByHashtag(hashtag, pageable).map(ArticleDto::from);
    }

    public List<String> getHashtags() {
        return articleRepository.findAllDistinctHashtags();
    }
}
