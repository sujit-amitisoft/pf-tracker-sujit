import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthPage } from "./AuthPage";

describe("AuthPage", () => {
  it("renders the login heading", () => {
    render(
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>,
    );
    expect(screen.getByText("Welcome back")).toBeDefined();
  });
});
