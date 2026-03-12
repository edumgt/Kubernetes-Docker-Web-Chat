import type {
  AdminOverview,
  AdminSession,
  ChatMessage,
  ConversationSummary,
  PrivateConversation,
  Profile,
  SearchUser
} from "../types/chat";

async function fetchJson<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: "include",
    ...init
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

export const api = {
  searchUsers: (query: string) =>
    fetchJson<SearchUser[]>(`/chat/search?query=${encodeURIComponent(query)}`),

  getConversations: () => fetchJson<ConversationSummary[]>("/chat/conversations/unread"),

  getOrCreatePrivateConversation: (recipient: string) =>
    fetchJson<PrivateConversation>(`/chat/conversations/private?recipient=${encodeURIComponent(recipient)}`),

  getPublicMessages: () => fetchJson<ChatMessage[]>("/chat/messages"),

  getPrivateMessages: (conversationId: string) =>
    fetchJson<ChatMessage[]>(`/chat/messages/${encodeURIComponent(conversationId)}`),

  markAsRead: (conversationId: string) =>
    fetch(`/chat/conversations/${encodeURIComponent(conversationId)}/read`, {
      method: "POST",
      credentials: "include"
    }),

  getProfile: (email: string) =>
    fetchJson<Profile>(`/profile/${encodeURIComponent(email)}`),

  adminLogin: (email: string, password: string) =>
    fetchJson<AdminSession>("/chat/admin/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ email, password })
    }),

  adminLogout: () =>
    fetch("/chat/admin/logout", {
      method: "POST",
      credentials: "include"
    }),

  getAdminMe: () => fetchJson<AdminSession>("/chat/admin/me"),

  getAdminOverview: () => fetchJson<AdminOverview>("/chat/admin/overview")
};
