import assert from "node:assert/strict";
import test from "node:test";

import { USERNAME_PATTERN } from "./authValidation.js";

test("username pattern is valid under modern HTML pattern semantics", () => {
  const usernameExpression = new RegExp(`^(?:${USERNAME_PATTERN})$`, "v");

  for (const username of ["jay", "jay-patel", "jay_patel", "jay.patel", "Jay0078"]) {
    assert.equal(usernameExpression.test(username), true, username);
  }

  for (const username of ["jay patel", "jay@patel", "jay/patel"]) {
    assert.equal(usernameExpression.test(username), false, username);
  }
});
