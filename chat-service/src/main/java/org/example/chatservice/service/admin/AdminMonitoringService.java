package org.example.chatservice.service.admin;

import org.example.chatservice.model.Conversation;
import org.example.chatservice.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class AdminMonitoringService {

    private final ConversationRepository conversationRepository;
    private final KubernetesApiService kubernetesApiService;

    public AdminMonitoringService(
            ConversationRepository conversationRepository,
            KubernetesApiService kubernetesApiService
    ) {
        this.conversationRepository = conversationRepository;
        this.kubernetesApiService = kubernetesApiService;
    }

    public AdminOverviewResponse getOverview() {
        long privateRoomCount = conversationRepository.count();
        long totalRoomCount = privateRoomCount + 1;
        long participantCount = countParticipantsInPrivateRooms();

        KubernetesApiService.KubernetesSnapshot snapshot = kubernetesApiService.fetchSnapshot();

        return new AdminOverviewResponse(
                Instant.now().toString(),
                snapshot.namespace(),
                snapshot.apiServer(),
                new ChatMetrics(totalRoomCount, privateRoomCount, participantCount),
                snapshot.nodes(),
                snapshot.pods(),
                snapshot.podPhaseSummary(),
                snapshot.errorMessage()
        );
    }

    private long countParticipantsInPrivateRooms() {
        Set<String> participants = new HashSet<>();
        for (Conversation conversation : conversationRepository.findAll()) {
            if (conversation.getUser1() != null && !conversation.getUser1().isBlank()) {
                participants.add(conversation.getUser1().trim());
            }
            if (conversation.getUser2() != null && !conversation.getUser2().isBlank()) {
                participants.add(conversation.getUser2().trim());
            }
        }
        return participants.size();
    }

    public record AdminOverviewResponse(
            String generatedAt,
            String namespace,
            String apiServer,
            ChatMetrics chatMetrics,
            java.util.List<KubernetesApiService.KubernetesNode> nodes,
            java.util.List<KubernetesApiService.KubernetesPod> pods,
            java.util.Map<String, Long> podPhaseSummary,
            String kubernetesError
    ) {}

    public record ChatMetrics(
            long totalRoomCount,
            long privateRoomCount,
            long participantCount
    ) {}
}
