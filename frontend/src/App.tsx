import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate, useParams } from "react-router-dom";
import { api } from "./lib/api";
import type {
  AdminOverview,
  ChatMessage,
  ConversationSummary,
  Profile,
  ReadReceiptEvent,
  SearchUser
} from "./types/chat";

const DEFAULT_IMG =
  "https://media.pitchfork.com/photos/5c7d4c1b4101df3df85c41e5/1:1/w_800,h_800,c_limit/Dababy_BabyOnBaby.jpg";

function formatTimestamp(value?: string) {
  if (!value) return "just now";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "just now";
  return date.toLocaleString();
}

function LoginRequired() {
  return (
    <section className="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-amber-900 shadow-lg">
      <h2 className="font-display text-3xl">로그인이 필요합니다</h2>
      <p className="mt-3 text-sm">
        OAuth 로그인 후 SPA를 바로 사용할 수 있습니다.
      </p>
      <a
        href="/auth/login"
        className="mt-5 inline-flex rounded-full bg-ink px-5 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
      >
        OAuth 로그인
      </a>
    </section>
  );
}

function AppShell({
  currentUser,
  children
}: {
  currentUser: string | null;
  children: ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_#fff7ed_0%,_#f8fafc_45%,_#ecfeff_100%)] pb-12 text-ink">
      <header className="sticky top-0 z-20 border-b border-white/60 bg-white/75 backdrop-blur">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-4 sm:px-8">
          <Link to="/" className="font-display text-2xl text-slate-900">
            Realtime Chat SPA
          </Link>
          <nav className="flex items-center gap-2">
            <Link
              to="/"
              className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-mint hover:text-mint"
            >
              Direct
            </Link>
            <Link
              to="/public-room"
              className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-mint hover:text-mint"
            >
              Public
            </Link>
            <Link
              to="/backoffice"
              className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-mint hover:text-mint"
            >
              Backoffice
            </Link>
            <a
              href="/profile"
              className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-mint hover:text-mint"
            >
              Profile
            </a>
            <a
              href="/auth/logout"
              className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              Logout
            </a>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-4 pt-8 sm:px-8">
        <div className="mb-5 rounded-2xl border border-white/70 bg-white/80 px-4 py-3 text-sm shadow">
          {currentUser ? `로그인 사용자: ${currentUser}` : "로그인 정보가 없습니다."}
        </div>
        {children}
      </main>
    </div>
  );
}

function DashboardPage({ currentUser }: { currentUser: string | null }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SearchUser[]>([]);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [profiles, setProfiles] = useState<Record<string, Profile>>({});
  const [isLoadingSearch, setIsLoadingSearch] = useState(false);
  const [conversationError, setConversationError] = useState<string | null>(null);

  const profileTargets = useMemo(() => {
    const targets = new Set<string>();
    searchResults.forEach((user) => targets.add(user.email));
    conversations.forEach((conversation) => targets.add(conversation.peerUser));
    return [...targets];
  }, [searchResults, conversations]);

  useEffect(() => {
    if (!currentUser) return;
    api
      .getConversations()
      .then((rows) => {
        setConversations(rows);
        setConversationError(null);
      })
      .catch(() => setConversationError("대화 목록을 불러오지 못했습니다."));
  }, [currentUser]);

  useEffect(() => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(() => {
      setIsLoadingSearch(true);
      api
        .searchUsers(query.trim())
        .then((rows) => setSearchResults(rows.filter((user) => user.email !== currentUser)))
        .finally(() => setIsLoadingSearch(false));
    }, 250);

    return () => clearTimeout(timer);
  }, [query, currentUser]);

  useEffect(() => {
    if (!profileTargets.length) return;

    const missing = profileTargets.filter((email) => !profiles[email]);
    if (!missing.length) return;

    Promise.all(
      missing.map(async (email) => {
        try {
          const profile = await api.getProfile(email);
          return [email, profile] as const;
        } catch {
          return [email, { email }] as const;
        }
      })
    ).then((entries) => {
      setProfiles((prev) => {
        const next = { ...prev };
        entries.forEach(([email, profile]) => {
          next[email] = profile;
        });
        return next;
      });
    });
  }, [profileTargets, profiles]);

  if (!currentUser) {
    return <LoginRequired />;
  }

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
        <p className="inline-flex rounded-full bg-haze px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-mint">
          Start DM
        </p>
        <h1 className="mt-3 font-display text-4xl">사용자 검색</h1>
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm focus-within:border-mint">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="email로 검색"
            className="w-full border-none bg-transparent text-sm outline-none placeholder:text-slate-400"
          />
        </div>
        {isLoadingSearch ? <p className="mt-3 text-sm text-slate-500">검색 중...</p> : null}

        <div className="mt-4 grid gap-3">
          {searchResults.map((user) => {
            const profile = profiles[user.email];
            return (
              <button
                key={user.email}
                type="button"
                onClick={() => navigate(`/dm/${encodeURIComponent(user.email)}`)}
                className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3 text-left transition hover:-translate-y-0.5 hover:border-mint hover:shadow-md"
              >
                <img
                  src={profile?.profilePicUrl || DEFAULT_IMG}
                  alt="profile"
                  className="h-12 w-12 rounded-xl object-cover"
                />
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-900">
                    {profile?.displayName || user.email}
                  </p>
                  <p className="truncate text-xs text-slate-500">{user.email}</p>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      <section className="rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-3xl">Recent Conversations</h2>
          <button
            type="button"
            onClick={() =>
              api
                .getConversations()
                .then((rows) => setConversations(rows))
                .catch(() => setConversationError("대화 목록을 불러오지 못했습니다."))
            }
            className="rounded-full border border-slate-300 px-3 py-1 text-xs font-semibold text-slate-700 hover:border-mint hover:text-mint"
          >
            Refresh
          </button>
        </div>
        {conversationError ? <p className="mb-3 text-sm text-rose-600">{conversationError}</p> : null}
        <div className="grid gap-3">
          {conversations.map((conversation) => {
            const profile = profiles[conversation.peerUser];
            return (
              <button
                key={conversation.conversationId}
                type="button"
                onClick={() => navigate(`/dm/${encodeURIComponent(conversation.peerUser)}`)}
                className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3 text-left transition hover:-translate-y-0.5 hover:border-mint hover:shadow-md"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <img
                    src={profile?.profilePicUrl || DEFAULT_IMG}
                    alt="profile"
                    className="h-12 w-12 rounded-xl object-cover"
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold">
                      {profile?.displayName || conversation.peerUser}
                    </p>
                    <p className="truncate text-xs text-slate-500">
                      {conversation.lastMessagePreview || "메시지가 없습니다"}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-[11px] text-slate-500">{formatTimestamp(conversation.lastMessageAt)}</p>
                  {conversation.unreadCount > 0 ? (
                    <span className="mt-1 inline-flex rounded-full bg-ember px-2.5 py-0.5 text-xs font-bold text-white">
                      {conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}
                    </span>
                  ) : null}
                </div>
              </button>
            );
          })}
          {!conversations.length ? (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
              최근 대화가 없습니다.
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function PublicRoomPage({ currentUser }: { currentUser: string | null }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!currentUser) return;

    api.getPublicMessages().then(setMessages).catch(() => setMessages([]));

    const client = new Client({
      webSocketFactory: () => new SockJS("/chat"),
      reconnectDelay: 2000
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe("/topic/messages", (frame) => {
        const message = JSON.parse(frame.body) as ChatMessage;
        setMessages((prev) => [...prev, message]);
      });
    };

    client.onDisconnect = () => setConnected(false);
    client.onStompError = () => setConnected(false);
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [currentUser]);

  const sendMessage = () => {
    if (!input.trim() || !clientRef.current || !currentUser) return;
    clientRef.current.publish({
      destination: "/app/sendMessage",
      body: JSON.stringify({
        sender: currentUser,
        content: input.trim()
      })
    });
    setInput("");
  };

  if (!currentUser) {
    return <LoginRequired />;
  }

  return (
    <section className="rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-4xl">Public Room</h1>
        <span
          className={
            connected
              ? "rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700"
              : "rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-700"
          }
        >
          {connected ? "Connected" : "Offline"}
        </span>
      </div>

      <div className="h-[56vh] min-h-[360px] space-y-3 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
        {messages.map((message, index) => (
          <article
            key={`${message.sender}-${message.timestamp ?? index}-${index}`}
            className="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm"
          >
            <p className="break-words text-sm text-slate-800">{message.content}</p>
            <p className="mt-2 text-xs text-slate-500">
              {message.sender} • {formatTimestamp(message.timestamp)}
            </p>
          </article>
        ))}
      </div>

      <div className="mt-4 flex gap-2">
        <input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") sendMessage();
          }}
          placeholder="메시지를 입력하세요"
          className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none focus:border-mint"
        />
        <button
          type="button"
          onClick={sendMessage}
          className="rounded-2xl bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          Send
        </button>
      </div>
    </section>
  );
}

function PrivateRoomPage({ currentUser }: { currentUser: string | null }) {
  const params = useParams<{ recipient: string }>();
  const recipient = params.recipient ? decodeURIComponent(params.recipient) : "";
  const [conversationId, setConversationId] = useState<string>("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const [readStatus, setReadStatus] = useState("Read status pending");
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!currentUser || !recipient) return;

    api
      .getOrCreatePrivateConversation(recipient)
      .then((conversation) => {
        setConversationId(conversation.conversationId);
        return api.getPrivateMessages(conversation.conversationId);
      })
      .then((rows) => setMessages(rows))
      .catch(() => {
        setMessages([]);
      });
  }, [currentUser, recipient]);

  useEffect(() => {
    if (!currentUser || !recipient || !conversationId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS("/chat"),
      reconnectDelay: 2000
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/user/queue/private/${conversationId}`, (frame) => {
        const message = JSON.parse(frame.body) as ChatMessage;
        setMessages((prev) => [...prev, message]);
        if (message.sender !== currentUser) {
          api.markAsRead(conversationId).catch(() => undefined);
        }
      });
      client.subscribe(`/user/queue/private/${conversationId}/read`, (frame) => {
        const readReceipt = JSON.parse(frame.body) as ReadReceiptEvent;
        if (readReceipt.reader === recipient) {
          setReadStatus(`Read by ${recipient} at ${formatTimestamp(readReceipt.readAt)}`);
        }
      });
      api.markAsRead(conversationId).catch(() => undefined);
    };

    client.onDisconnect = () => setConnected(false);
    client.onStompError = () => setConnected(false);
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [currentUser, recipient, conversationId]);

  const sendMessage = () => {
    if (!input.trim() || !clientRef.current || !currentUser || !recipient) return;
    clientRef.current.publish({
      destination: "/app/private/chat",
      body: JSON.stringify({
        sender: currentUser,
        recipient,
        content: input.trim()
      })
    });
    setInput("");
  };

  if (!currentUser) {
    return <LoginRequired />;
  }

  if (!recipient) {
    return <Navigate to="/" replace />;
  }

  return (
    <section className="rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-display text-4xl">Chat with {recipient}</h1>
        <div className="flex gap-2">
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">{readStatus}</span>
          <span
            className={
              connected
                ? "rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700"
                : "rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-700"
            }
          >
            {connected ? "Connected" : "Offline"}
          </span>
        </div>
      </div>

      <div className="h-[56vh] min-h-[360px] space-y-3 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
        {messages.map((message, index) => {
          const isMine = message.sender === currentUser;
          return (
            <article
              key={`${message.sender}-${message.timestamp ?? index}-${index}`}
              className={isMine ? "flex justify-end" : "flex justify-start"}
            >
              <div
                className={
                  isMine
                    ? "max-w-[80%] rounded-2xl rounded-br-sm bg-slate-900 px-4 py-3 text-sm text-white shadow"
                    : "max-w-[80%] rounded-2xl rounded-bl-sm bg-white px-4 py-3 text-sm text-slate-800 shadow"
                }
              >
                <p className="break-words">{message.content}</p>
                <p className={isMine ? "mt-2 text-[11px] text-slate-300" : "mt-2 text-[11px] text-slate-500"}>
                  {message.sender} • {formatTimestamp(message.timestamp)}
                </p>
              </div>
            </article>
          );
        })}
      </div>

      <div className="mt-4 flex gap-2">
        <input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") sendMessage();
          }}
          placeholder="메시지를 입력하세요"
          className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none focus:border-mint"
        />
        <button
          type="button"
          onClick={sendMessage}
          className="rounded-2xl bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          Send
        </button>
      </div>
    </section>
  );
}

function MetricTile({ title, value, description }: { title: string; value: string | number; description: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">{title}</p>
      <p className="mt-2 text-3xl font-bold text-slate-900">{value}</p>
      <p className="mt-1 text-xs text-slate-500">{description}</p>
    </article>
  );
}

function BackofficePage() {
  const [email, setEmail] = useState("admin@test.com");
  const [password, setPassword] = useState("123456");
  const [adminEmail, setAdminEmail] = useState<string | null>(null);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [isLoading, setIsLoading] = useState(false);

  const loadOverview = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await api.getAdminOverview();
      setOverview(data);
    } catch {
      setErrorMessage("모니터링 데이터를 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const session = await api.getAdminMe();
        if (!mounted) return;
        setAdminEmail(session.email);
        const data = await api.getAdminOverview();
        if (!mounted) return;
        setOverview(data);
      } catch {
        if (!mounted) return;
        setAdminEmail(null);
      } finally {
        if (mounted) {
          setIsCheckingSession(false);
        }
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    setIsLoading(true);
    try {
      const session = await api.adminLogin(email.trim(), password);
      setAdminEmail(session.email);
      const data = await api.getAdminOverview();
      setOverview(data);
    } catch {
      setErrorMessage("관리자 로그인에 실패했습니다. 계정을 확인하세요.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    await api.adminLogout();
    setAdminEmail(null);
    setOverview(null);
    setEmail("admin@test.com");
    setPassword("123456");
  };

  const runningPods = overview?.podPhaseSummary?.Running ?? 0;
  const readyNodes = overview?.nodes?.filter((node) => node.ready).length ?? 0;

  if (isCheckingSession) {
    return (
      <section className="rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
        <p className="text-sm text-slate-600">백오피스 세션을 확인하는 중입니다...</p>
      </section>
    );
  }

  if (!adminEmail) {
    return (
      <section
        data-testid="admin-login-page"
        className="mx-auto max-w-lg rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl"
      >
        <p className="inline-flex rounded-full bg-haze px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-mint">
          Chat Backoffice
        </p>
        <h1 className="mt-3 font-display text-4xl text-slate-900">Admin Login</h1>
        <p className="mt-2 text-sm text-slate-600">
          Kubernetes API 기반 Node/Pod 모니터링, 채팅방/참여자 지표를 확인합니다.
        </p>
        <form className="mt-6 space-y-4" onSubmit={handleLogin}>
          <div>
            <label htmlFor="admin-email" className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
              Email
            </label>
            <input
              id="admin-email"
              name="admin-email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none focus:border-mint"
            />
          </div>
          <div>
            <label htmlFor="admin-password" className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
              Password
            </label>
            <input
              id="admin-password"
              name="admin-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none focus:border-mint"
            />
          </div>
          {errorMessage ? <p className="text-sm text-rose-600">{errorMessage}</p> : null}
          <button
            type="submit"
            data-testid="admin-login-button"
            disabled={isLoading}
            className="w-full rounded-2xl bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLoading ? "로그인 중..." : "Login"}
          </button>
        </form>
      </section>
    );
  }

  return (
    <section data-testid="admin-dashboard" className="space-y-6 rounded-3xl border border-white/70 bg-white/85 p-6 shadow-xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="inline-flex rounded-full bg-haze px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-mint">
            Chat Backoffice
          </p>
          <h1 className="mt-2 font-display text-4xl text-slate-900">Kubernetes + Chat Monitoring</h1>
          <p className="mt-2 text-sm text-slate-600">
            로그인 계정: <span className="font-semibold">{adminEmail}</span> | namespace:{" "}
            <span className="font-semibold">{overview?.namespace ?? "-"}</span>
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={loadOverview}
            className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-mint hover:text-mint"
          >
            Refresh
          </button>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            Logout
          </button>
        </div>
      </div>

      {errorMessage ? <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{errorMessage}</p> : null}
      {overview?.kubernetesError ? (
        <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">{overview.kubernetesError}</p>
      ) : null}

      <div className="grid gap-3 md:grid-cols-4">
        <MetricTile
          title="Chat Rooms"
          value={overview?.chatMetrics.totalRoomCount ?? 0}
          description={`private: ${overview?.chatMetrics.privateRoomCount ?? 0}, public 포함`}
        />
        <MetricTile
          title="Participants"
          value={overview?.chatMetrics.participantCount ?? 0}
          description="개인 채팅 기준 유니크 참여자"
        />
        <MetricTile
          title="Nodes Ready"
          value={`${readyNodes}/${overview?.nodes.length ?? 0}`}
          description="Kubernetes API /api/v1/nodes"
        />
        <MetricTile
          title="Pods Running"
          value={`${runningPods}/${overview?.pods.length ?? 0}`}
          description="Kubernetes API /api/v1/namespaces/{ns}/pods"
        />
      </div>

      {isLoading ? <p className="text-sm text-slate-600">데이터 새로고침 중...</p> : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="font-display text-2xl text-slate-900">Nodes</h2>
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="py-2 pr-3 font-semibold">Name</th>
                  <th className="py-2 pr-3 font-semibold">Role</th>
                  <th className="py-2 pr-3 font-semibold">Status</th>
                  <th className="py-2 pr-3 font-semibold">Version</th>
                </tr>
              </thead>
              <tbody>
                {(overview?.nodes ?? []).map((node) => (
                  <tr key={node.name} className="border-b border-slate-100">
                    <td className="py-2 pr-3 text-slate-800">{node.name}</td>
                    <td className="py-2 pr-3 text-slate-600">{node.role}</td>
                    <td className="py-2 pr-3">
                      <span
                        className={
                          node.ready
                            ? "rounded-full bg-emerald-100 px-2 py-1 text-[11px] font-semibold text-emerald-700"
                            : "rounded-full bg-rose-100 px-2 py-1 text-[11px] font-semibold text-rose-700"
                        }
                      >
                        {node.status}
                      </span>
                    </td>
                    <td className="py-2 pr-3 text-slate-600">{node.kubeletVersion || "-"}</td>
                  </tr>
                ))}
                {!overview?.nodes.length ? (
                  <tr>
                    <td className="py-3 text-slate-500" colSpan={4}>
                      노드 정보가 없습니다.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </article>

        <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="font-display text-2xl text-slate-900">Pods ({overview?.namespace ?? "-"})</h2>
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="py-2 pr-3 font-semibold">Name</th>
                  <th className="py-2 pr-3 font-semibold">Phase</th>
                  <th className="py-2 pr-3 font-semibold">Ready</th>
                  <th className="py-2 pr-3 font-semibold">Node</th>
                </tr>
              </thead>
              <tbody>
                {(overview?.pods ?? []).map((pod) => (
                  <tr key={pod.name} className="border-b border-slate-100">
                    <td className="py-2 pr-3 text-slate-800">{pod.name}</td>
                    <td className="py-2 pr-3 text-slate-600">{pod.phase}</td>
                    <td className="py-2 pr-3 text-slate-600">{pod.ready}</td>
                    <td className="py-2 pr-3 text-slate-600">{pod.nodeName || "-"}</td>
                  </tr>
                ))}
                {!overview?.pods.length ? (
                  <tr>
                    <td className="py-3 text-slate-500" colSpan={4}>
                      파드 정보가 없습니다.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </article>
      </div>
    </section>
  );
}

function RootApp() {
  const [currentUser, setCurrentUser] = useState<string | null>(null);

  useEffect(() => {
    fetch("/chat/me", { credentials: "include" })
      .then((response) => (response.ok ? response.json() : null))
      .then((data) => {
        if (data && data.username) {
          setCurrentUser(data.username as string);
        } else {
          setCurrentUser(null);
        }
      })
      .catch(() => setCurrentUser(null));
  }, []);

  return (
    <BrowserRouter>
      <AppShell currentUser={currentUser}>
        <Routes>
          <Route path="/" element={<DashboardPage currentUser={currentUser} />} />
          <Route path="/public-room" element={<PublicRoomPage currentUser={currentUser} />} />
          <Route path="/dm/:recipient" element={<PrivateRoomPage currentUser={currentUser} />} />
          <Route path="/backoffice" element={<BackofficePage />} />
        </Routes>
      </AppShell>
    </BrowserRouter>
  );
}

export default RootApp;
