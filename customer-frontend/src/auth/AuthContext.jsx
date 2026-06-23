import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

const STORAGE_KEYS = {
  TOKEN: "customer_auth_token",
  CUSTOMER_EMAIL: "customer_email",
  CUSTOMER_ROLE: "customer_role",
};

const API_BASE = `${import.meta.env.VITE_API_BASE_URL}/auth/customer`;

const getToken = () => localStorage.getItem(STORAGE_KEYS.TOKEN);
const getEmail = () => localStorage.getItem(STORAGE_KEYS.CUSTOMER_EMAIL);
const getRole = () => localStorage.getItem(STORAGE_KEYS.CUSTOMER_ROLE) || "CUSTOMER";

export function AuthProvider({ children }) {
  const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem(STORAGE_KEYS.TOKEN));
  const [email, setEmail] = useState(getEmail);
  const [role, setRole] = useState(getRole);
  const [customer, setCustomer] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // ❌ NO useEffect — localStorage is managed directly in login/signup/logout below

  const signup = async (payload) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_BASE}/signup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        credentials: "include",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Signup failed (${response.status})`);
      }

      const data = await response.json();

      localStorage.setItem(STORAGE_KEYS.TOKEN, data.token);
      localStorage.setItem(STORAGE_KEYS.CUSTOMER_EMAIL, data.email);
      localStorage.setItem(STORAGE_KEYS.CUSTOMER_ROLE, data.role || "CUSTOMER");

      setIsLoggedIn(true);
      setEmail(data.email);
      setRole(data.role || "CUSTOMER");
      setCustomer(data);

      return data;
    } catch (err) {
      const errorMsg = err.message || "Signup failed. Please try again.";
      setError(errorMsg);
      throw new Error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const login = async (email_input, password) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_BASE}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: email_input, password }),
        credentials: "include",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Invalid email or password");
      }

      const data = await response.json();

      localStorage.setItem(STORAGE_KEYS.TOKEN, data.token);
      localStorage.setItem(STORAGE_KEYS.CUSTOMER_EMAIL, data.email);
      localStorage.setItem(STORAGE_KEYS.CUSTOMER_ROLE, data.role || "CUSTOMER");

      setIsLoggedIn(true);
      setEmail(data.email);
      setRole(data.role || "CUSTOMER");
      setCustomer(data);

      return data;
    } catch (err) {
      const errorMsg = err.message || "Login failed. Please try again.";
      setError(errorMsg);
      throw new Error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.CUSTOMER_EMAIL);
    localStorage.removeItem(STORAGE_KEYS.CUSTOMER_ROLE);
    setIsLoggedIn(false);
    setEmail(null);
    setRole("CUSTOMER");
    setCustomer(null);
    setError(null);
  };

  const value = {
    isAuthenticated: isLoggedIn,
    isLoggedIn,
    email,
    role,
    customer,
    loading,
    error,
    signup,
    login,
    logout,
    getToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}

export { AuthContext, STORAGE_KEYS };