const pexelsHome = "https://www.pexels.com/";

function pexelsPageUrl(value) {
  if (typeof value !== "string") return null;
  try {
    const url = new URL(value);
    if (url.protocol !== "https:") return null;
    if (url.hostname !== "pexels.com" && url.hostname !== "www.pexels.com") {
      return null;
    }
    return url.href;
  } catch {
    return null;
  }
}

export default function PexelsAttribution({ photo, className = "" }) {
  const imageUrl = typeof photo?.imageUrl === "string" ? photo.imageUrl : "";
  if (!/^https:\/\/images\.pexels\.com\//i.test(imageUrl)) return null;

  const sourceUrl = pexelsPageUrl(photo.imageSourceUrl) || pexelsHome;
  const photographerUrl = pexelsPageUrl(photo.imagePhotographerUrl);
  const photographer =
    typeof photo.imagePhotographer === "string"
      ? photo.imagePhotographer.trim()
      : "";

  return (
    <p
      className={`text-sm text-slate-600 ${className}`.trim()}
      onClick={(event) => event.stopPropagation()}
    >
      {photographer ? (
        <>
          Photo by{" "}
          {photographerUrl ? (
            <a
              href={photographerUrl}
              target="_blank"
              rel="noreferrer"
              className="underline hover:text-slate-700"
            >
              {photographer}
            </a>
          ) : (
            photographer
          )}{" "}
          on{" "}
        </>
      ) : (
        "Photo provided by "
      )}
      <a
        href={sourceUrl}
        target="_blank"
        rel="noreferrer"
        className="underline hover:text-slate-700"
      >
        Pexels
      </a>
    </p>
  );
}
