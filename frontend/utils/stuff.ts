import { Item, Offer } from "./model";
import _ from "underscore";

export const toSafePathPart = (text: string): string => text.toLowerCase().replace(/[^0-9a-z]/g, "-");

export const extractOffer = (item: Item): Offer => {
  const offer = _.first(item.offers);
  if (offer === undefined) {
    throw new Error("No offers for item");
  }
  return offer;
}
