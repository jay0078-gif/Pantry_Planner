import { useState } from "react";
import { recipeCover, recipeImage } from "../lib/images";
import PexelsAttribution from "./PexelsAttribution";

export default function RecipePhoto({
  photo,
  containerClassName = "",
  imageClassName = "",
  attributionClassName = "",
  loading,
  decoding = "async",
}) {
  const fallback = recipeCover(photo?.name);
  const desiredSource = recipeImage(photo?.name, photo?.imageUrl);
  const [failedSource, setFailedSource] = useState("");
  const source = failedSource === desiredSource ? fallback : desiredSource;
  const isShowingPexels = /^https:\/\/images\.pexels\.com\//i.test(source);

  return (
    <>
      <div className={containerClassName}>
        <img
          src={source}
          alt={photo?.name || "Recipe"}
          className={imageClassName}
          loading={loading}
          decoding={decoding}
          onError={() => setFailedSource(desiredSource)}
        />
      </div>
      {isShowingPexels && (
        <PexelsAttribution photo={photo} className={attributionClassName} />
      )}
    </>
  );
}
