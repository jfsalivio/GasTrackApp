import { API_BASE_URL } from '../api';
const apiBase = API_BASE_URL;
const apiImage = path => window.GasTrackAndroid
  ? `${window.location.origin}/backend/${path.replace(/^\/+/, '')}`
  : `${apiBase}/${path.replace(/^\/+/, '')}`;

const accessoryImages = [
  [/hose clamp.*1\.5/i, "acc-lpg-hoseclamp-1.5.png"],
  [/hose clamp/i, "acc-lpg-hoseclamp.png"],
  [/hose/i, "acc-lpg-hose.png"],
  [/tpa.*regulator/i, "acc-tpa-regulator.png"],
  [/pol.*regulator/i, "acc-pol-regulator.png"],
  [/reyna.*stove/i, "acc-reyna-gasstove.png"],
];

export const productImagePlaceholder = "data:image/svg+xml," + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><rect width="160" height="160" rx="12" fill="#f3f4f6"/><rect x="48" y="40" width="64" height="64" rx="8" fill="none" stroke="#9ca3af" stroke-width="3"/><circle cx="68" cy="60" r="7" fill="#9ca3af"/><path d="m50 94 22-22 16 16 12-12 10 18" fill="none" stroke="#9ca3af" stroke-width="3"/><text x="80" y="130" text-anchor="middle" font-family="sans-serif" font-size="12" fill="#6b7280">No image available</text></svg>'
);

export function resolveProductImage(product) {
  const raw = product?.ImageURL || product?.imageUrl || product?.Imageurl || product?.imageURL;
  if (raw) {
    const url = String(raw).trim().replace(/\\/g, "/");
    if (/^(data:|blob:)/i.test(url)) return url;
    if (/^https?:/i.test(url)) {
      const parsed = new URL(url);
      if (parsed.pathname.startsWith('/uploads/') && (parsed.origin === new URL(apiBase).origin || ['localhost', '127.0.0.1', '10.0.2.2'].includes(parsed.hostname))) return apiImage(parsed.pathname);
      return url;
    }
    const path = url.replace(/^\/+/, "");
    // These assets are served by Vite, including deployments under a base path.
    if (path.startsWith("assets/products/")) return `${import.meta.env.BASE_URL}${path}`;
    return apiImage(path);
  }

  const name = product?.ProductName || product?.name || "";
  const accessory = accessoryImages.find(([pattern]) => pattern.test(name));
  if (accessory) return apiImage(`uploads/${accessory[1]}`);

  const size = name.match(/(2\.7|7|11|22|50)\s*kg/i)?.[1];
  if (size && /gasul|cylinder/i.test(name)) {
    const type = /cylinder/i.test(name) ? "cylinder" : "gasul";
    const variant = /elite/i.test(name) && size === "11" ? "elite11" : size;
    return apiImage(`uploads/${type}-${variant}kg.png`);
  }
  return productImagePlaceholder;
}
