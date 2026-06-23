import { STORAGE_KEYS } from "../auth/AuthContext";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

/**
 * Fetch wrapper that automatically attaches JWT token to requests
 * This replaces the old unauthenticated fetch calls
 */
export async function apiCall(url, options = {}) {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
  
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  // Attach JWT token if available
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
    credentials: "include", // For CORS with authentication
  };

  try {
    const response = await fetch(url, config);

    // If 401, token is expired or invalid - logout user
    if (response.status === 401) {
      // Remove the localStorage wipe and window.location.href
      // Just throw — let the component show an error
      throw new Error("Session expired. Please login again.");
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Request failed (${response.status})`);
    }

    return await response.json();
  } catch (error) {
    console.error("API call error:", error);
    throw error;
  }
}

/**
 * GET request helper
 */
export async function apiGet(endpoint) {
  const url = `${API_BASE_URL}${endpoint}`;
  return apiCall(url, { method: "GET" });
}

/**
 * POST request helper
 */
export async function apiPost(endpoint, data) {
  const url = `${API_BASE_URL}${endpoint}`;
  return apiCall(url, {
    method: "POST",
    body: JSON.stringify(data),
  });
}

/**
 * PUT request helper
 */
export async function apiPut(endpoint, data) {
  const url = `${API_BASE_URL}${endpoint}`;
  return apiCall(url, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

/**
 * DELETE request helper
 */
export async function apiDelete(endpoint) {
  const url = `${API_BASE_URL}${endpoint}`;
  return apiCall(url, { method: "DELETE" });
}

export default {
  call: apiCall,
  get: apiGet,
  post: apiPost,
  put: apiPut,
  delete: apiDelete,
};
