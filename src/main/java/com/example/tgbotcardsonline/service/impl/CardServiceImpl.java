package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.CardRepository;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.validator.MoveValidator;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardsClient cardsClient;
    private final DeckResponseRepository deckResponseRepository;
    private final TelegramBot telegramBot;
    private final GameRepository gameRepository;
    private final CardRepository cardRepository;
    private final MoveValidator moveValidator;

    @Override
    public String brandNewDeck() {
        DeckResponse deckResponse = cardsClient.contactToPartialDeck();
        deckResponseRepository.save(deckResponse);
        log.info(deckResponse.toString());
        return deckResponse.getDeck_id();
    }

    @Override
    public DrawCardsResponse drawACardFromCardsClient(String deckId, int howMany) {
        try {
            DrawCardsResponse drawCardsResponse = cardsClient.contactToDrawACard(deckId, howMany);
            DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
            deckResponse.setRemaining(drawCardsResponse.getRemaining());
            deckResponseRepository.save(deckResponse);
            return drawCardsResponse;
        } catch (Exception e) {
            throw new RuntimeException("Error drawing cards: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Card> drawCards(Game game, int howMany) {
        List<Card> cards = game.getCards();
        List<Card> askedCards = new ArrayList<>();

        if (howMany > cards.size()) {
            howMany = cards.size();
        }

        for (int i = 0; i < howMany; i++) {
            Card c = cards.get(i);
            askedCards.add(c);
        }

        cards.removeAll(askedCards);
        game.setCards(cards);
        gameRepository.save(game);
        return askedCards;
    }


    @Override
    public Card getInputtedCardByCodeAndGame(OnlinePlayer onlinePlayer, String callBackData) {
        Optional<Card> cardByCodeAndGame = cardRepository.findCardByCodeAndGameId(callBackData, onlinePlayer.getGame().getId());
        if(cardByCodeAndGame.isPresent()){
            return cardByCodeAndGame.get();
        }
         telegramBot.sendMessageToPlayer(onlinePlayer.getPlayer(), "You do not have card " + callBackData);
        throw new RuntimeException();
    }
}
