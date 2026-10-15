package com.complaint.complaint_management_system.service;

import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NlpKeywordExtractionServiceImpl
        implements NlpKeywordExtractionService {

    private final Tokenizer tokenizer;
    private final POSTagger posTagger;
    private final Lemmatizer lemmatizer;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the",
            "is", "are", "was", "were",
            "be", "been", "being",
            "has", "have", "had",
            "and", "or", "but",
            "if", "then",
            "states",
            "stated",
            "earlier",
            "above",
            "below",
            "hereby",
            "therefore",
            "therein",
            "matter",
            "issue",
            "request",
            "requests",
            "requested",
            "than",
            "of", "in", "on", "at", "to",
            "for", "from", "with", "by",
            "about",
            "this", "that", "these", "those",
            "it", "its",
            "there", "here",
            "my", "our", "your",
            "they", "them", "we", "you"
    );

    public NlpKeywordExtractionServiceImpl() {

        try {

            ClassPathResource tokenizerResource =
                    new ClassPathResource(
                            "models/opennlp-en-ud-ewt-tokens-1.3-2.5.4.bin"
                    );

            InputStream tokenizerInputStream =
                    tokenizerResource.getInputStream();

            TokenizerModel tokenizerModel =
                    new TokenizerModel(tokenizerInputStream);

            tokenizer =
                    new TokenizerME(tokenizerModel);


            ClassPathResource posResource =
                    new ClassPathResource(
                            "models/opennlp-en-ud-ewt-pos-1.3-2.5.4.bin"
                    );

            InputStream posInputStream =
                    posResource.getInputStream();

            POSModel posModel =
                    new POSModel(posInputStream);

            posTagger =
                    new POSTaggerME(posModel);


            ClassPathResource lemmaResource =
                    new ClassPathResource(
                            "models/opennlp-en-ud-ewt-lemmas-1.3-2.5.4.bin"
                    );

            InputStream lemmaInputStream =
                    lemmaResource.getInputStream();

            LemmatizerModel lemmaModel =
                    new LemmatizerModel(lemmaInputStream);

            lemmatizer =
                    new LemmatizerME(lemmaModel);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to load OpenNLP NLP models",
                    e
            );
        }
    }


    @Override
    public List<String> extractKeywords(
            String subject,
            String description) {

        String text =
                (subject == null ? "" : subject)
                        + " "
                        + (description == null ? "" : description);

        text = text.trim();

        if (text.isEmpty()) {
            return List.of();
        }

        String[] tokens =
                tokenizer.tokenize(text);

        String[] posTags =
                posTagger.tag(tokens);

        String[] lemmas =
                lemmatizer.lemmatize(tokens, posTags);


        List<String> keywords =
                new ArrayList<>();


        for (int i = 0; i < tokens.length; i++) {

            String pos =
                    posTags[i];

            String word =
                    lemmas[i].toLowerCase();


            // Keep only NOUN, VERB and ADJECTIVE

            if (!pos.equals("NOUN")
                    && !pos.equals("VERB")
                    && !pos.equals("ADJ")) {

                continue;
            }

            word = word.replaceAll(
                    "[^a-z0-9]",
                    ""
            );

            if (word.isEmpty()) {
                continue;
            }

            if (word.length() <= 2) {
                continue;
            }

            if (STOP_WORDS.contains(word)) {
                continue;
            }


            keywords.add(word);
        }

        return keywords.stream()
                .distinct()
                .collect(Collectors.toList());
    }
}