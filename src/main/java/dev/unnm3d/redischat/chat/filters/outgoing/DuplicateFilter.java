package dev.unnm3d.redischat.chat.filters.outgoing;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.settings.FiltersConfig;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;


public class DuplicateFilter extends AbstractFilter<DuplicateFilter.DuplicateFilterProperties> {

    public DuplicateFilter(DuplicateFilterProperties filterSettings) {
        super("duplicate", Direction.OUTGOING, filterSettings);
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        //Skip filter if no previous messages
        if(previousMessages.length == 0) return new FilterResult(message, false, Optional.empty());

        //Reverse the array to check the last messages first
        int checkedMessages = 0;
        for (ChatMessage chatMessage :
                Arrays.stream(previousMessages)
                        .sorted(Comparator.comparingLong(ChatMessage::getTimestamp).reversed())
                        .toList()) {
            float similarityPercentage = levenshteinScore(message.getContent(), chatMessage.getContent()) * 100;

            //If the similarity is lower than the percentage, skip filter
            if (similarityPercentage < filterSettings.similarityPercentage) {
                return new FilterResult(message, false, Optional.empty());
            }
            if (++checkedMessages == filterSettings.messagesToCheck) break;
        }

        return new FilterResult(message, true,
                Optional.of(RedisChat.getInstance().getComponentProvider()
                        .parse(sender, RedisChat.getInstance().messages.duplicate_message, true,
                                false, false)));
    }

    private float levenshteinScore(String first, String second) {
        int maxLength = Math.max(first.length(), second.length());
        //Can't divide by 0
        if (maxLength == 0) return 1.0f;
        return ((float) (maxLength - computeEditDistance(first, second))) / (float) maxLength;
    }

    private int computeEditDistance(String first, String second) {
        first = first.toLowerCase();
        second = second.toLowerCase();

        int[] costs = new int[second.length() + 1];
        for (int i = 0; i <= first.length(); i++) {
            int previousValue = i;
            for (int j = 0; j <= second.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int useValue = costs[j - 1];
                    if (first.charAt(i - 1) != second.charAt(j - 1)) {
                        useValue = Math.min(Math.min(useValue, previousValue), costs[j]) + 1;
                    }
                    costs[j - 1] = previousValue;
                    previousValue = useValue;

                }
            }
            if (i > 0) {
                costs[second.length()] = previousValue;
            }
        }
        return costs[second.length()];
    }


    @Configuration
    @Getter
    public static class DuplicateFilterProperties extends FiltersConfig.FilterSettings {
        private int similarityPercentage;
        @Comment("How many messages to check for duplicates (from the last one)")
        private int messagesToCheck;

        public DuplicateFilterProperties() {
            super(true, 8, Set.of(), Set.of());
            this.similarityPercentage = 60;
            this.messagesToCheck = 2;
        }
    }
}
